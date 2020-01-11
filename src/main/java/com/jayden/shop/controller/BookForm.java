package com.jayden.shop.controller;

import com.jayden.shop.domain.item.Book;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class BookForm {

    private Long id;

    private String name;
    private int price;
    private int stockQuantity;

    private String author;
    private String isbn;

    public static BookForm of(Book entity) {
        BookForm bookForm = new BookForm();
        bookForm.id = entity.getId();
        bookForm.name = entity.getName();
        bookForm.price = entity.getPrice();
        bookForm.stockQuantity = entity.getStockQuantity();
        bookForm.author = entity.getAuthor();
        bookForm.isbn = entity.getIsbn();
        return bookForm;
    }
}
