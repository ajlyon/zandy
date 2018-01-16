package com.gimranov.zandy.app.api;

import com.gimranov.zandy.app.model.Collection;
import com.gimranov.zandy.app.model.Item;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okhttp3.ResponseBody;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import okio.BufferedSource;
import okio.Okio;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import static com.gimranov.zandy.app.api.CallTestUtil.getValue;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/*
 * This file is part of Zandy.
 *
 * Zandy is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Zandy is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Zandy.  If not, see <http://www.gnu.org/licenses/>.
 */
@RunWith(JUnit4.class)
public class ZoteroServiceTest {
    private ZoteroService mZoteroService;
    private MockWebServer mMockWebServer;

    @Before
    public void setUp() throws Exception {
        mMockWebServer = new MockWebServer();
        Gson gson = new GsonBuilder()
                .registerTypeAdapter(Item.class, new ZoteroDeserializer())
                .registerTypeAdapter(Collection.class, new ZoteroDeserializer())
                .create();

        mZoteroService = new Retrofit.Builder()
                .baseUrl(mMockWebServer.url("/"))
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build()
                .create(ZoteroService.class);
    }

    @After
    public void tearDown() throws Exception {
        mMockWebServer.shutdown();
    }

    @Test
    public void getItemForUser() throws Exception {
        enqueueResponse("item.json");
        Item item = getValue(mZoteroService.getItemForUser("475425", "X42A7DEE"));
        RecordedRequest request = mMockWebServer.takeRequest();
        assertEquals("/475425/items/X42A7DEE", request.getPath() );
        assertNotNull(item);
        assertEquals("Bristol, UK", item.getPlace());
        assertEquals("Institute of Physics conference series", item.getSeries());
    }

    @Test
    public void getCollectionsForUser() throws Exception {
        enqueueResponse("collections.json");
        List<Collection> collections = getValue(mZoteroService.getCollectionsForUser("475425"));
        RecordedRequest request = mMockWebServer.takeRequest();
        assertEquals("/475425/collections", request.getPath());
        assertEquals(15, collections.size());
        Collection collection = collections.get(0);
        assertEquals("LoC", collection.getName());
    }

    @Test
    public void getItemsForUser() throws Exception {
        enqueueResponse("items.json");
        List<Item> items = getValue(mZoteroService.getItemsForUser("475425"));
        RecordedRequest request = mMockWebServer.takeRequest();
        assertEquals("/475425/items", request.getPath());
        assertEquals(25, items.size());
        Item item = items.get(0);
        assertEquals("Zotero Blog » Blog Archive » A Unified Zotero Experience", item.getTitle());
    }

    @Test
    public void getItemKeysForUser() throws Exception {
        enqueueResponse("itemKeys.txt");
        Response<ResponseBody> response = mZoteroService.getItemKeysForUser("475425").execute();
        String body = response.body().string();
        RecordedRequest request = mMockWebServer.takeRequest();
        List<String> keys = Arrays.asList(body.split("[\r\n]+"));
        assertEquals("/475425/items?format=keys", request.getPath());
        assertEquals(240, keys.size());
        assertEquals("4TZXUMKG", keys.get(177));

    }

    private void enqueueResponse(String fileName) throws IOException {
        enqueueResponse(fileName, Collections.<String, String>emptyMap());
    }

    private void enqueueResponse(String fileName, Map<String, String> headers) throws IOException {
        InputStream inputStream = getClass().getClassLoader()
                                            .getResourceAsStream("api-response/" + fileName);
        BufferedSource source = Okio.buffer(Okio.source(inputStream));
        MockResponse mockResponse = new MockResponse();
        for (Map.Entry<String, String> header : headers.entrySet()) {
            mockResponse.addHeader(header.getKey(), header.getValue());
        }
        mMockWebServer.enqueue(mockResponse
                .setBody(source.readString(StandardCharsets.UTF_8)));
    }

}