package functional

import functional.tests.Book
import functional.tests.BookClient

import static org.springframework.http.HttpStatus.*

class BookClientController {

    static responseFormats = ['json', 'xml']
    static allowedMethods = [save: "POST", update: "PUT", delete: "DELETE"]

    def index(Integer max) {
        params.max = Math.min(max ?: 10, 100)
        respond BookClient.list()
    }

    def show(Long id) {
        respond BookClient.get(id)
    }

// TODO: The API for handling Rx write operations not implemented yet
//    def save(Book book) {
//        if (book == null) {
//            render status: NOT_FOUND
//            return
//        }
//
//        if (book.hasErrors()) {
//            respond book.errors, view:'create'
//            return
//        }
//
//        book.save flush:true
//
//        respond book, [status: CREATED, view:"show"]
//    }
//
//    def update(Book book) {
//        if (book == null) {
//            render status: NOT_FOUND
//            return
//        }
//
//        if (book.hasErrors()) {
//            respond book.errors, view:'edit'
//            return
//        }
//
//        book.save flush:true
//
//        respond book, [status: OK, view:"show"]
//    }
//
//    def delete(Long id) {
//        BookClient.get(id)
//                  .switchMap { Book book ->
//            book.delete()
//        }
//        if (book == null) {
//            render status: NOT_FOUND
//            return
//        }
//
//        book.delete flush:true
//
//        render status: NO_CONTENT
//    }
}