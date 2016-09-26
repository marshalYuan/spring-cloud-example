const fetch = require('isomorphic-fetch')

const SIDECAR = {
    uri: 'http://localhost:8741'
}

const USER_SERVICE = 'user-service'

exports.getUserServiceInfo = () => fetch(`${SIDECAR.uri}/hosts/${USER_SERVICE}`).then((resp)=>resp.json())

exports.getUserById = (id) => fetch(`${SIDECAR.uri}/${USER_SERVICE}/${id}`).then((resp)=>resp.json())
