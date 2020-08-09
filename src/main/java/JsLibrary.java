import java.util.Objects;

public class JsLibrary {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        JsLibrary jsLibrary = (JsLibrary) o;
        return Objects.equals(name, jsLibrary.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }
}
