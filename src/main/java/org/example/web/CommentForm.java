package org.example.web;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

public class CommentForm {
    @NotBlank
    @Size(max = 1000)
    private String content;

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
}
