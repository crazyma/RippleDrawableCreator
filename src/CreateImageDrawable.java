import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import form.AndroidSelectorDialog;
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

    final String drawableDirStr = "drawable";
    private static final String INDENT_SPACE = "{http://xml.apache.org/xslt}indent-amount";
    private static final String nsUri = "http://www.w3.org/2000/xmlns/";
    private static final String androidUri = "http://schemas.android.com/apk/res/android";

    @Override
    public void actionPerformed(AnActionEvent anActionEvent) {
        System.out.println("FUCK!");

        final VirtualFile dir = anActionEvent.getData(LangDataKeys.VIRTUAL_FILE);
        if (dir == null) {
            System.out.println("FUCK!!!!!!!!!!!");
            return;
        }

        VirtualFile resDir = getResDir(dir);

        if(isSelectedFileValid(dir) && resDir!= null){
            Project project = anActionEvent.getProject();
            AndroidSelectorDialog dialog = new AndroidSelectorDialog(project, resDir, dir);
            dialog.show();
        }


//        Application application = ApplicationManager.getApplication();
//
//        application.runWriteAction(() -> {
//            System.out.println("action!");
//            try {
//                generate(dir);
//                System.out.println("create drawable done");
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (ParserConfigurationException e) {
//                e.printStackTrace();
//            } catch (TransformerException e) {
//                e.printStackTrace();
//            }
//        });

    }

    private String getOutputFileName(String selectedFileName){
        return "ripple_" + selectedFileName + ".xml";
    }

    private boolean isSelectedFileValid(VirtualFile dir){
        String regex = ".*((\\.png)|(\\.jpg))|(\\.9.png)$";
        return dir.getName().matches(regex);
    }

    private VirtualFile getResDir(VirtualFile dir){
        VirtualFile resDir;
        do{
            resDir = dir.getParent();
            dir = resDir;
        }while(resDir.getName().contains("drawable"));

        if(resDir != null && resDir.getName().equals("res"))
            return resDir;
        return null;
    }

    private VirtualFile getDrawableDir(VirtualFile resDir) throws IOException {
        VirtualFile drawableDir = resDir.findChild(drawableDirStr);
        if(drawableDir == null)
            drawableDir = resDir.createChildDirectory(null, drawableDirStr);

        return drawableDir;
    }

    private void generate(VirtualFile dir) throws IOException, ParserConfigurationException, TransformerException {
        System.out.println("dir to stirng : " + dir.getName());
        VirtualFile resDir;
        String imageFileName = dir.getName();
        imageFileName = imageFileName.substring(0,imageFileName.indexOf("."));
        String outputDrawableFileName = "ripple_" + imageFileName + ".xml";

        do{
            resDir = dir.getParent();
            dir = resDir;
        }while(resDir.getName().contains("drawable"));

        if(resDir == null || !resDir.getName().equals("res")){
            return;
        }

        VirtualFile drawableDir = resDir.findChild(drawableDirStr);
        if(drawableDir == null)
            drawableDir = dir.createChildDirectory(null, drawableDirStr);


        VirtualFile newXmlFile = drawableDir.findChild(outputDrawableFileName);
        if (newXmlFile != null && newXmlFile.exists()) {
            newXmlFile.delete(null);
        }

        newXmlFile = drawableDir.createChildData(null, outputDrawableFileName);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();
        Element root = doc.createElement("ripple");
        root.setAttributeNS(nsUri, "xmlns:android", androidUri);
        root.setAttribute("android:color","#777777");
        doc.appendChild(root);

        Element item = doc.createElement("item");
        item.setAttribute("android:drawable",
                "@drawable/" + imageFileName);
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
