package org.example.web;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class ArticleForm {
    @NotBlank
    @Size(max = 140)
    private String title;

    @NotBlank
    @Size(max = 4000)
    private String content;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
