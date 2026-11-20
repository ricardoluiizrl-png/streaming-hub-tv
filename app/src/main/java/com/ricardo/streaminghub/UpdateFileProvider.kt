package com.ricardo.streaminghub

import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.Environment
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

/**
 * Exposes only APK files in this app's private external Downloads directory.
 * Read-only access is granted to the package installer through URI permissions.
 */
class UpdateFileProvider : ContentProvider() {

    override fun onCreate(): Boolean = true

    override fun getType(uri: Uri): String = APK_MIME_TYPE

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor {
        val file = resolveApk(uri)
        val columns = projection ?: arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE)
        val cursor = MatrixCursor(columns)
        val row = cursor.newRow()
        columns.forEach { column ->
            when (column) {
                OpenableColumns.DISPLAY_NAME -> row.add(file.name)
                OpenableColumns.SIZE -> row.add(file.length())
                else -> row.add(null)
            }
        }
        return cursor
    }

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if (mode != "r") throw FileNotFoundException("Somente leitura é permitida.")
        val file = resolveApk(uri)
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? =
        throw UnsupportedOperationException("Provider somente leitura.")

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int =
        throw UnsupportedOperationException("Provider somente leitura.")

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = throw UnsupportedOperationException("Provider somente leitura.")

    private fun resolveApk(uri: Uri): File {
        if (uri.authority != "${context?.packageName}.fileprovider") {
            throw FileNotFoundException("Autoridade inválida.")
        }
        val segments = uri.pathSegments
        if (segments.size != 2 || segments[0] != "downloads" ||
            !segments[1].endsWith(".apk", ignoreCase = true)
        ) {
            throw FileNotFoundException("Caminho inválido.")
        }

        val root = context?.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: throw FileNotFoundException("Diretório de atualização indisponível.")
        val canonicalRoot = root.canonicalFile
        val file = File(canonicalRoot, segments[1]).canonicalFile
        if (file.parentFile != canonicalRoot || !file.isFile) {
            throw FileNotFoundException("APK não encontrado.")
        }
        return file
    }

    companion object {
        private const val APK_MIME_TYPE = "application/vnd.android.package-archive"
    }
}
