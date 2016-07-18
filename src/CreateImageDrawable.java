import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.vfs.VirtualFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.*;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Created by david on 2016/7/18.
 */
public class CreateImageDrawable extends AnAction {

    final String drawableDir = "drawable";
    private static final String INDENT_SPACE = "{http://xml.apache.org/xslt}indent-amount";
    private static final String nsUri = "http://www.w3.org/2000/xmlns/";
    private static final String androidUri = "http://schemas.android.com/apk/res/android";

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        final VirtualFile dir = anActionEvent.getData(LangDataKeys.VIRTUAL_FILE);


        if (dir == null) {
            return;
        }

        try {
            generate(dir,"newDrawable.xml");
            System.out.println("create drawable done");
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (TransformerException e) {
            e.printStackTrace();
        }

    }

    private void generate(VirtualFile dir, String filename) throws IOException, ParserConfigurationException, TransformerException {
        VirtualFile child = dir.findChild(drawableDir);
        if(child == null)
            child = dir.createChildDirectory(null, drawableDir);

        VirtualFile newXmlFile = child.findChild(filename);
        if (newXmlFile != null && newXmlFile.exists()) {
            newXmlFile.delete(null);
        }

        newXmlFile = child.createChildData(null, filename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();
        Element root = doc.createElement("selector");
        root.setAttributeNS(nsUri, "xmlns:android", androidUri);
        doc.appendChild(root);

        Element item = doc.createElement("item");
        item.setAttribute("android:drawable",
                "@drawable/abc_list_selector_disabled_holo_light");
        item.setAttribute("android:state_enabled", "false");
        item.setAttribute("android:state_focused", "true");
        item.setAttribute("android:state_pressed", "true");
        root.appendChild(item);

        OutputStream os = newXmlFile.getOutputStream(null);
        PrintWriter out = new PrintWriter(os);

        StringWriter writer = new StringWriter();
        TransformerFactory tf = TransformerFactory.newInstance();
        Transformer transformer = tf.newTransformer();
        transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(INDENT_SPACE, "4");
        transformer.transform(new DOMSource(doc), new StreamResult(writer));

        out.println(writer.getBuffer().toString());
        out.close();

    }

}
