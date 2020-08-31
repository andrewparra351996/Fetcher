package org.bibletranslationtools.fetcher.usecase

import java.io.File
import org.bibletranslationtools.fetcher.repository.BookRepository
import org.bibletranslationtools.fetcher.repository.ChapterCatalog
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.viewdata.BookViewData
import org.bibletranslationtools.fetcher.usecase.viewdata.ChapterViewData

class FetchBookViewData(
    private val bookRepo: BookRepository,
    private val storage: StorageAccess,
    private val languageCode: String
) {
    private val books = bookRepo.getBooks(languageCode = languageCode, resourceId = "ulb")

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    fun getViewDataList(
        currentPath: String,
        productSlug: String,
        chapterCatalog: ChapterCatalog
    ): List<BookViewData> {
        val bookViewList = mutableListOf<BookViewData>()

        books.forEach {
            val bookViewData = getViewData(it.slug, productSlug)
            val chapters = FetchChapterViewData(
                chapterCatalog = chapterCatalog,
                storage = storage,
                languageCode = languageCode,
                productSlug = productSlug,
                bookSlug = it.slug
            ).getViewDataList()

            var chapterAvailability = false
            chapters?.forEach { chapter -> if(chapter.url != null) chapterAvailability = true }

            val availability = it.availability && chapterAvailability && bookViewData?.url != null

            bookViewList.add(
                BookViewData(
                    index = it.index,
                    slug = it.slug,
                    anglicizedName = it.anglicizedName,
                    localizedName = it.localizedName,
                    url = if (availability) "$currentPath/${it.slug}" else null
                )
            )
        }

        return bookViewList
    }

    fun getViewData(bookSlug: String, productSlug: String): BookViewData? {
        val product = ProductFileExtension.getType(productSlug) ?: return null
        val book = bookRepo.getBook(bookSlug, languageCode) ?: return null
        var url: String? = null

        for (priority in priorityList) {
            val fileAccessRequest = when (product) {
                ProductFileExtension.BTTR -> getBTTRFileAccessRequest(bookSlug, priority)
                ProductFileExtension.MP3 -> getMp3FileAccessRequest(bookSlug, priority)
            }

            val bookFile = storage.getBookFile(fileAccessRequest)
            if (bookFile != null) {
                url = getBookDownloadUrl(bookFile)
                break
            }
        }

        return BookViewData(
            index = book.index,
            slug = book.slug,
            anglicizedName = book.anglicizedName,
            localizedName = book.localizedName,
            url = url
        )
    }

    private fun getBTTRFileAccessRequest(
        bookSlug: String,
        priorityItem: PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = "tr",
            bookSlug = bookSlug,
            mediaExtension = priorityItem.fileExtension,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getMp3FileAccessRequest(
        bookSlug: String,
        priorityItem: PriorityItem
    ): FileAccessRequest {
        return FileAccessRequest(
            languageCode = languageCode,
            resourceId = "ulb",
            fileExtension = priorityItem.fileExtension,
            bookSlug = bookSlug,
            mediaQuality = priorityItem.mediaQuality
        )
    }

    private fun getBookDownloadUrl(bookFile: File): String {
        val relativeBookPath = bookFile.relativeTo(storage.getContentRoot()).invariantSeparatorsPath
        return "//${System.getenv("CDN_BASE_URL")}/$relativeBookPath"
    }
}
