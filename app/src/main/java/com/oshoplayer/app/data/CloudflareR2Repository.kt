package com.oshoplayer.app.data

import aws.sdk.kotlin.runtime.auth.credentials.StaticCredentialsProvider
import aws.sdk.kotlin.services.s3.S3Client
import aws.sdk.kotlin.services.s3.model.GetObjectRequest
import aws.sdk.kotlin.services.s3.model.ListObjectsRequest
import aws.sdk.kotlin.services.s3.presigners.presignGetObject
import aws.smithy.kotlin.runtime.auth.awscredentials.Credentials
import aws.smithy.kotlin.runtime.net.url.Url
import aws.sdk.kotlin.services.s3.model.PutObjectRequest
import aws.smithy.kotlin.runtime.content.ByteStream
import kotlin.time.Duration.Companion.hours

data class RemoteAudio(
    val key: String,
    val title: String,
    val folder: String,
    val sizeBytes: Long
)

class CloudflareR2Repository {
    private val bucketName = "osho"
    
    private val s3Client = S3Client {
        region = "us-east-1"
        endpointUrl = Url.parse("YOUR_R2_ENDPOINT_URL_HERE")
        credentialsProvider = StaticCredentialsProvider(
            Credentials(
                accessKeyId = "YOUR_R2_ACCESS_KEY_ID_HERE",
                secretAccessKey = "YOUR_R2_SECRET_ACCESS_KEY_HERE"
            )
        )
        forcePathStyle = true
    }

    suspend fun listDiscourses(): List<RemoteAudio> {
        val request = ListObjectsRequest {
            bucket = bucketName
        }
        val response = s3Client.listObjects(request)
        
        val items = response.contents
        if (items.isNullOrEmpty()) {
            throw RuntimeException("R2 returned empty contents. XML parsing failed or bucket empty.")
        }
        
        val unsorted = items.mapNotNull { obj ->
            val key = obj.key ?: return@mapNotNull null
            // Skip folders or empty keys
            if (key.endsWith("/")) return@mapNotNull null
            if (key.startsWith("screenshots/")) return@mapNotNull null
            
            val folderRaw = if (key.contains("/")) key.substringBeforeLast("/") else "Other"
            val folder = folderRaw.replace("_", " ")
            
            // Clean up the key to get a readable title
            val title = key.substringAfterLast("/").substringBeforeLast(".")
            RemoteAudio(
                key = key,
                title = title,
                folder = folder,
                sizeBytes = obj.size ?: 0L
            )
        }
        
        return unsorted.sortedWith(compareBy({ it.folder }, { extractNumberForSort(it.title) }, { it.title }))
    }

    private fun extractNumberForSort(title: String): Int {
        val match = "\\d+".toRegex().find(title)
        return match?.value?.toIntOrNull() ?: 0
    }

    suspend fun getStreamingUrl(key: String): String {
        val request = GetObjectRequest {
            bucket = bucketName
            this.key = key
        }
        val presignedRequest = s3Client.presignGetObject(request, 12.hours)
        return presignedRequest.url.toString()
    }

    suspend fun uploadScreenshot(fileName: String, data: ByteArray): String {
        val request = PutObjectRequest {
            bucket = bucketName
            key = "screenshots/$fileName"
            body = ByteStream.fromBytes(data)
            contentLength = data.size.toLong()
            contentType = "image/jpeg"
        }
        s3Client.putObject(request)
        return "screenshots/$fileName"
    }
}
