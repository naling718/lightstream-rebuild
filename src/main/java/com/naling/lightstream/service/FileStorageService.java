package com.naling.lightstream.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class FileStorageService {
    private final S3Client s3Client;
    private final String bucketName;

    public FileStorageService(S3Client s3Client, @Value("${lightstream.storage.bucket}") String bucketName){
        this.s3Client = s3Client;
        this.bucketName = bucketName;

        try{
            s3Client.headBucket(r -> r.bucket(bucketName)); //send a HEAD request: HEAD http://localhost:9000/lightstream-files
        } catch (NoSuchBucketException e){
            s3Client.createBucket(r -> r.bucket(bucketName));
        }
    }

    public void upload(MultipartFile file){ // browser -> multipart request -> Spring ──MultipartFile
        String objKey = UUID.randomUUID().toString();

        try (InputStream in = file.getInputStream()){
            s3Client.putObject(
                    r -> r.bucket(bucketName).key(objKey).contentType(file.getContentType()),
                    RequestBody.fromInputStream(in, file.getSize())
            ); // anything declared in the try(...) is closed automatically
        } catch (IOException e){
            throw new UncheckedIOException("Could not read the uploaded file", e);
        }

    }

    public void delete(String objKey){
        s3Client.deleteObject(r -> r.bucket(bucketName).key(objKey));
    }

    public ResponseInputStream<GetObjectResponse> download(String objKey){
        return s3Client.getObject(r -> r.bucket(bucketName).key(objKey));
    }

    // The cleanup job uses this list to find leftover files that should be deleted
    public List<String> listObjKeysOlderThan(Instant cutoff){
        List<String> keys = new ArrayList<>();
        for (S3Object obj : s3Client.listObjectsV2Paginator(r -> r.bucket(bucketName)).contents()){ // Paginator will go to the next page automatically
            if (obj.lastModified().isBefore(cutoff)){
                keys.add(obj.key());
            }
        }
        return keys;
    }
}
