package com.example.appengine.springboot.web;

import com.google.api.core.ApiFuture;
import com.google.api.gax.core.FixedCredentialsProvider;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
public class TestController {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestController.class);

    private Firestore db;

    @PostConstruct
    public void getFireStoreConnection() {
        String env = System.getenv("env");

        if (env != null && env.equals("local") && db == null) {
            /**
             * Use local emulator
             *
             * https://github.com/googleapis/java-firestore/issues/361
             *
             * start the emulator using the firebase emulators:start
             * NOTE: this will wipe data on every start of emulator
             *
             * in below java api for emulator pass the projectId and host:port
             * used while setting up the emulator.
             * *********************************************
             *
             * - to start firestore emulator using gcloud
             * gcloud beta emulators firestore start
             * - deploy with a version number, else gcloud will generate an internal id
             * gcloud app deploy --version=0.0.1
             * - command to list all services
             * gcloud app services list
             * - list all the versions of the service
             * gcloud app versions list
             * - deploy a different service, use diff app.yaml
             * gcloud app deploy --appyaml=src/main/appengine/test_service.yaml --version=0-0-0-2
             */
            Firestore FS = FirestoreOptions.getDefaultInstance().toBuilder()
                    .setProjectId("test-compute-engine-300420")
                    .setHost("localhost:8085")
                    .setCredentials(new FirestoreOptions.EmulatorCredentials())
                    .setCredentialsProvider(FixedCredentialsProvider.create(new FirestoreOptions.EmulatorCredentials()))
                    .build()
                    .getService();
            System.out.println(FS.document("doc/test"));
            this.db = FS;
            LOGGER.info(">>> Connected to firestore");
        } else if (db == null) {
            FirestoreOptions firestoreOptions =
                    FirestoreOptions.newBuilder()
                            .setProjectId("test-compute-engine-300420")
                            .build();
            this.db = firestoreOptions.getService();
            LOGGER.info(">>> Connected to firestore");
        }
    }

    @GetMapping("/hello")
    public String hello(@RequestParam(value = "name", defaultValue = "World") String name) {
        LOGGER.info(">>> start hello request");
        try {
            connectFireStore();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info(">>> end hello request");
        return String.format("Hello %s!", name);
    }

    public void connectFireStore() throws IOException {
        //Firestore db = FirestoreOptions.getDefaultInstance().getService();
    /*FirestoreOptions firestoreOptions =
            FirestoreOptions.newBuilder()
                    .setEmulatorHost("localhost:8085")
                    .build();
    Firestore db = firestoreOptions.getService();

    this.db = db;

    String emulatorHost = db.getOptions().getEmulatorHost();
    System.out.println(emulatorHost);*/

        // Create a Map to store the data we want to set
        Map<String, Object> docData = new HashMap<>();
        docData.put("name", "Los Angeles");
        docData.put("state", "CA");
        docData.put("country", "USA");
        docData.put("regions", Arrays.asList("west_coast", "socal"));
// Add a new document (asynchronously) in collection "cities" with id "LA"
        ApiFuture<WriteResult> future = db.collection("cities").document("LA").set(docData);
// ...
// future.get() blocks on response
        try {
            System.out.println("Update time : " + future.get().getUpdateTime());
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }

        DocumentReference document = db.collection("cities").document("LA");
        ApiFuture<DocumentSnapshot> documentSnapshotApiFuture = document.get();
        DocumentSnapshot documentSnapshot = null;
        try {
            documentSnapshot = documentSnapshotApiFuture.get();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (ExecutionException e) {
            e.printStackTrace();
        }
        System.out.println(documentSnapshot.getData());
        System.out.println(db.document("cities/LA"));
    }

    @GetMapping("/data")
    public String getData(@RequestParam(value = "collection") String collection,
                          @RequestParam(value = "document") String document) throws ExecutionException, InterruptedException {
        LOGGER.info(">>> start data request collection={}, document={}", collection, document);
        ApiFuture<DocumentSnapshot> future = db.collection(collection).document(document).get();
        DocumentSnapshot snapshot = future.get();
        LOGGER.info(">>> data returned {}", snapshot.getData().toString());
        return snapshot.getData().toString();
    }
}
