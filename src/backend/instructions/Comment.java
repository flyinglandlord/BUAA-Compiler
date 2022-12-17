package backend.instructions;

public class Comment implements Instructions {
    private String comment;

    public Comment(String comment) {
        this.comment = comment;
    }

    public String toString() {
        return "# " + comment;
    }
}
