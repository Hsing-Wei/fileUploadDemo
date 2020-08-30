package com.example.fileUploadDemo;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import static org.hamcrest.Matchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.util.LinkedMultiValueMap;

import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Path;
import java.util.List;

@RunWith(SpringRunner.class)
@SpringBootTest
@AutoConfigureMockMvc
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class FileControllerTests {

    @Autowired
    private MockMvc mockMvc;
    private String fileName;
    private String path = "E:\\tmp\\";
    private String md5Value;
    private List<Path> partFiles;
    private List<String> fileList;

    @Before
    public void init() throws Exception{
        fileName = "apache-tomcat-7.0.94-windows-x64.zip";
        md5Value = SplitFiles.getFileMd5(new File("E:\\tmp\\" + fileName));
        partFiles = SplitFiles.splitFile("E:\\tmp\\" + fileName, 1);
    }

    @After
    public void clear() {
        for (Path p:partFiles) {
            File f = p.toFile();
            f.delete();
        }
    }

    @Test
    public void t1_testCheckFile() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("fileName", fileName);
        requestParams.add("fileMd5Value", md5Value);
        MvcResult result = mockMvc.perform(get("/check/file")
                .params(requestParams))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.stat", is(1)))
                .andReturn();
        String response = result.getResponse().getContentAsString();
        fileList = JsonPath.parse(response).read("$.chunkList[*]");
    }

    @Test
    public void t2_testUploadFile() throws Exception {
        for (Path p:partFiles) {
            File f = p.toFile();
                FileInputStream fi = new FileInputStream(f);
                MockMultipartFile fmp = new MockMultipartFile("data", f.getName(), "multipart/form-data", fi);
                mockMvc.perform(MockMvcRequestBuilders.multipart("/upload")
                        .file(fmp)
                        .param("total", String.valueOf(partFiles.size()))
                        .param("index", String.valueOf(p.getFileName()))
                        .param("fileMd5Value", md5Value)
                        .accept(MediaType.APPLICATION_JSON))
                        .andDo(print())
                        .andExpect(status().is(200))
                        .andExpect(jsonPath("$.stat", is(1)))
                        .andExpect(jsonPath("$.desc", is(String.valueOf(p.getFileName()))));
            }
        }

    @Test
    public void t3_testMerge() throws Exception {
        LinkedMultiValueMap<String, String> requestParams = new LinkedMultiValueMap<>();
        requestParams.add("fileName", fileName);
        requestParams.add("md5", md5Value);
        mockMvc.perform(get("/merge")
                .params(requestParams))
                .andDo(print())
                .andExpect(status().is(200))
                .andExpect(jsonPath("$.stat", is(1)));
    }
}
