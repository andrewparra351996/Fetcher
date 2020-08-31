package org.bibletranslationtools.fetcher.repository

import java.io.File

interface StorageAccess {
    fun getContentRoot(): File
    fun getLanguageCodes(): List<String>
    fun getBookSlugs(languageCode: String, resourceId: String): List<String>
    fun getBookFile(request: FileAccessRequest): File?
    fun getChapterFile(request: FileAccessRequest): File?
    fun hasBookContent(
        languageCode: String,
        resourceId: String,
        bookSlug: String,
        mediaExtensionList: List<String>
    ): Boolean
}
