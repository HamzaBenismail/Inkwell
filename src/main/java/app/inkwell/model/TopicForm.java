package app.inkwell.model;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class TopicForm {

    @NotEmpty(message = "Topic title is required")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotEmpty(message = "Topic content is required")
    private String content;

    private String tags;  // JSON array of tags as string

    private boolean pinned = false;

    private boolean locked = false;
}