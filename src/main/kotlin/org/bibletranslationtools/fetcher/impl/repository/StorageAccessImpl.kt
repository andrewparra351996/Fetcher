package org.bibletranslationtools.fetcher.impl.repository

import org.bibletranslationtools.fetcher.data.Book
import java.io.File
import org.bibletranslationtools.fetcher.data.CompressedExtensions
import org.bibletranslationtools.fetcher.data.ContainerExtensions
import org.bibletranslationtools.fetcher.repository.DirectoryProvider
import org.bibletranslationtools.fetcher.repository.FileAccessRequest
import org.bibletranslationtools.fetcher.repository.StorageAccess
import org.bibletranslationtools.fetcher.usecase.ProductFileExtension
import org.slf4j.LoggerFactory

class StorageAccessImpl(private val directoryProvider: DirectoryProvider) : StorageAccess {
    private val logger = LoggerFactory.getLogger(javaClass)

    private data class PriorityItem(val fileExtension: String, val mediaQuality: String)

    private val priorityList = listOf(
        PriorityItem("mp3", "hi"),
        PriorityItem("mp3", "low"),
        PriorityItem("wav", "")
    )

    override fun getContentRoot(): File {
        return directoryProvider.getContentRoot()
    }

    override fun getLanguageCodes(): List<String> {
        val sourceFileRootDir = directoryProvider.getContentRoot()
        val dirs = sourceFileRootDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getBookSlugs(languageCode: String, resourceId: String): List<String> {
        val projectsDir = directoryProvider.getProjectsDir(languageCode, resourceId)
        val dirs = projectsDir.listFiles(File::isDirectory)

        return if (dirs.isNullOrEmpty()) listOf() else dirs.map { it.name }
    }

    override fun getBookFile(request: FileAccessRequest): File? {
        val bookPrefixDir = getPathPrefixDir(
            languageCode = request.languageCode,
            resourceId = request.resourceId,
            bookSlug = request.bookSlug,
            fileExtension = request.fileExtension
        )

        val grouping = "book"
        val bookContentDir = getContentDir(
            prefixDir = bookPrefixDir,
            fileExtension = request.fileExtension,
            mediaExtension = request.mediaExtension,
            mediaQuality = request.mediaQuality,
            grouping = grouping
        )

        return try {
            bookContentDir.listFiles(File::isFile)?.single()
        } catch (e: NoSuchElementException) {
            // no content
            null
        } catch (e: IllegalArgumentException) {
            // there are more than 1 file under the dir
            logger.error("Max files allowed: 1. Too many files found at $bookContentDir", e)
            null
        }
    }

    override fun isBookAvailable(
        book: Book,
        languageCode: String,
        resourceId: String,
        productSlug: String
    ): Boolean {
        val product = ProductFileExtension.getType(productSlug) ?: return false

        val bookPrefixDir = getPathPrefixDir(
            languageCode = languageCode,
            resourceId = resourceId,
            bookSlug = book.slug,
            fileExtension = product.fileType
        )

        return book.availability
    }

    override fun getChapterFile(request: FileAccessRequest): File? {
        val chapterPrefixDir = getPathPrefixDir(
            languageCode = request.languageCode,
            resourceId = request.resourceId,
            bookSlug = request.bookSlug,
            fileExtension = request.fileExtension,
            chapter = request.chapter
        )

        val grouping = "chapter"
        val chapterContentDir = getContentDir(
            prefixDir = chapterPrefixDir,
            fileExtension = request.fileExtension,
            mediaExtension = request.mediaExtension,
            mediaQuality = request.mediaQuality,
            grouping = grouping
        )

        return try {
            chapterContentDir.listFiles(File::isFile)?.single()
        } catch (e: NoSuchElementException) {
            // no content
            null
        } catch (e: IllegalArgumentException) {
            // there are more than 1 file under the dir
            logger.error("Max files allowed: 1. Too many files found at $chapterContentDir", e)
            null
        }
    }

    private fun getPathPrefixDir(
        languageCode: String,
        resourceId: String,
        fileExtension: String,
        bookSlug: String = "",
        chapter: String = ""
    ): File {
        val trimmedChapter = chapter.trimStart('0')
        val sourceContentRootDir = directoryProvider.getContentRoot()

        return when {
            bookSlug.isNotEmpty() && trimmedChapter.isNotEmpty() ->
                sourceContentRootDir.resolve(
                    "$languageCode/$resourceId/$bookSlug/$trimmedChapter/CONTENTS/$fileExtension"
                )
            bookSlug.isNotEmpty() -> sourceContentRootDir.resolve(
                "$languageCode/$resourceId/$bookSlug/CONTENTS/$fileExtension"
            )
            else -> sourceContentRootDir.resolve(
                "$languageCode/$resourceId/CONTENTS/$fileExtension"
            )
        }
    }

    private fun getContentDir(
        prefixDir: File,
        fileExtension: String,
        mediaExtension: String,
        mediaQuality: String,
        grouping: String
    ): File {
        val isContainer = ContainerExtensions.isSupported(fileExtension)
        val isContainerAndCompressed = isContainer && CompressedExtensions.isSupported(mediaExtension)
        val isFileAndCompressed = !isContainer && CompressedExtensions.isSupported(fileExtension)

        return prefixDir.resolve(
            when {
                isContainerAndCompressed -> "$mediaExtension/$mediaQuality/$grouping"
                isContainer -> "$mediaExtension/$grouping"
                isFileAndCompressed -> "$mediaQuality/$grouping"
                else -> grouping
            }
        )
    }
}
