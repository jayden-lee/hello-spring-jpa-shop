package com.jayden.shop.service;

import com.jayden.shop.controller.BookForm;
import com.jayden.shop.domain.item.Book;
import com.jayden.shop.domain.item.Item;
import com.jayden.shop.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    @Transactional
    public void saveItem(Item item) {
        itemRepository.save(item);
    }

    public List<Item> findItems() {
        return itemRepository.findAll();
    }

    public Item findOne(Long itemId) {
        return itemRepository.findOne(itemId);
    }

    @Transactional
    public void updateItem(Long itemId, BookForm form) {
        Book findItem = (Book) itemRepository.findOne(itemId);
        findItem.setPrice(form.getPrice());
        findItem.setName(form.getName());
        findItem.setStockQuantity(form.getStockQuantity());
        findItem.setAuthor(form.getAuthor());
        findItem.setIsbn(form.getIsbn());
    }
}
