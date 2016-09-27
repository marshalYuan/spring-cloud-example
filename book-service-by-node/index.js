const express = require('express')
const faker = require('faker/locale/zh_CN')
const logger = require('morgan')
const services = require('./service')

const app = express()


let count = 100
const books = new Array(count)

while (count > 0) {
    books[count] = {
        id: count,
        name: faker.name.title(),
        authorId: parseInt(Math.random() * 100) + 1,
        publishDate: faker.date.past().toLocaleString(),
        des: faker.lorem.paragraph(),
        ISBN: `ISBN 000-0000-00-0`
    }
    count --
}

app.use(logger('combined'))

app.get('/health', (req, res) => {
    res.json({
        status: 'UP'
    })
})

app.get('/book/:id', (req, res, next) => {
    const id = parseInt(req.params.id)
    if(isNaN(id)){
        next()
    }
    res.json(books[id])
})

app.get('/book/:bookId/author', (req, res, next) => {
    const bookId = parseInt(req.params.bookId)
    if(isNaN(bookId)){
        next()
    }
    const book = books[bookId]
    if(book) {
        let uid = book.authorId
        services.getUserById(uid).then((user) => {
            if(user.id) {
                res.json(user)
            }else{
                throw new Error("user not found")
            }
        }).catch((error)=> next(error))
    }
})

app.get('/books', (req, res, next) => {
    const uid = req.query.uid
    res.json(books.filter((book)=>book.authorId == uid))
})

app.get('/config', (req, res) => {
    services.getConfig('user-service').then((config) => {
        res.json(config)
    }).catch((e)=> next(e))
})

app.use((error, req, res, next) => {
    const status = 500 || error.status; 
    res.status(status)
    res.json({status, message: 'service error'})
})


app.listen(3000, () => console.log("express-application listening on 3000"))
