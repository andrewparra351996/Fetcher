package org.bibletranslationtools.fetcher.repository

import org.bibletranslationtools.fetcher.data.Book
import java.io.File

interface StorageAccess {
    fun getContentRoot(): File
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String, resourceId: String): List<String>
    fun getBookFile(request: FileAccessRequest): File?
    fun isBookAvailable(book: Book, languageCode: String, resourceId: String, productSlug: String): Boolean
    fun getChapterFile(request: FileAccessRequest): File?
}
