import java.util.Comparator;

public class JsLibraryComparator implements Comparator<JsLibrary> {
    @Override
    public int compare(JsLibrary lib1, JsLibrary lib2) {
        return lib1.getMd5().compareTo(lib2.getMd5());
    }
}
