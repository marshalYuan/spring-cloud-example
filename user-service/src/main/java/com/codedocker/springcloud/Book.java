package com.codedocker.springcloud;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by marshal on 16/9/27.
 */
class Book {
    private Long id;
    private Long authorId;
    private String name;
    private Date publishDate;
    private String des;
    private String ISBN;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAuthorId() {
        return authorId;
    }

    public void setAuthorId(Long authorId) {
        this.authorId = authorId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Date getPublishDate() {
        return publishDate;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public void setPublishDate(String date) {
        DateFormat format = new SimpleDateFormat("M/d/yyyy, hh:mm:ss", Locale.CHINESE);
        try {
            this.publishDate = format.parse(date);
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public String getDes() {
        return des;
    }

    public void setDes(String des) {
        this.des = des;
    }

    public String getISBN() {
        return ISBN;
    }

    public void setISBN(String ISBN) {
        this.ISBN = ISBN;
    }
}