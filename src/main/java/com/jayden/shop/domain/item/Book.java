package com.jayden.shop.domain.item;

import com.jayden.shop.controller.BookForm;
import lombok.Getter;
import lombok.Setter;

import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

@Entity
@DiscriminatorValue("B")
@Getter
@Setter
public class Book extends Item {

    private String author;
    private String isbn;

    public static Book of(BookForm form) {
        Book book = new Book();
        book.id = form.getId();
        book.name = form.getName();
        book.price = form.getPrice();
        book.stockQuantity = form.getStockQuantity();
        book.author = form.getAuthor();
        book.isbn = form.getIsbn();
        return book;
    }
}
