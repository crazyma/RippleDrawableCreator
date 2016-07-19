package com.crazyma.form;

import com.crazyma.color.ColorItemRenderer;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.swing.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathFactory;
import java.io.*;
import java.util.*;

public class AndroidSelectorDialog extends DialogWrapper {

    private static final String INDENT_SPACE = "{http://xml.apache.org/xslt}indent-amount";

    private static final String drawableDir = "drawable";
    private static final String drawableV21Dir = "drawable-v21";
    private static final String valuesColorsXml = "values/colors.xml";
    private static final String nsUri = "http://www.w3.org/2000/xmlns/";
    private static final String androidUri = "http://schemas.android.com/apk/res/android";

    private final Project project;

    private JPanel contentPane;
    private JTextField filenameText;
    private JComboBox colorCombo;
    private VirtualFile resDir,targetImageFile;
    private String targetImage;

    public AndroidSelectorDialog(@Nullable Project project, VirtualFile resDir, VirtualFile targetImageFile) {
        super(project);

        this.project = project;
        this.resDir = resDir;
        this.targetImageFile = targetImageFile;
        setTitle("Android Selector");
        setResizable(false);
        init();
    }

    @Override
    public void show() {
        try {
            if (initColors(resDir)) {
                initTargetImageAndOutputFileName();
                super.show();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initTargetImageAndOutputFileName(){
        String name = targetImageFile.getName();
        targetImage = name.substring(0,name.indexOf("."));
        filenameText.setText("selected_" + targetImage);
    }

    private boolean initColors(VirtualFile dir) {
        VirtualFile colorsXml = dir.findFileByRelativePath(valuesColorsXml);
        if (colorsXml != null && colorsXml.exists()) {
            HashMap<String, String> cmap = parseColorsXml(colorsXml);

            if (cmap.isEmpty()) {
                String title = "Error";
                String msg = "Cannot find colors in colors.xml";
                showMessageDialog(title, msg);
                return false;
            }

            String regex = "^@(android:)?color/(.+$)";
            ArrayList<String[]> elements = new ArrayList<String[]>();
            for (String name : cmap.keySet()) {
                String color = cmap.get(name);
                while (color != null && color.matches(regex)) {
                    if (color.startsWith("@color/")) {
                        String key = color.replace("@color/", "");
                        color = cmap.get(key);
                    } else {
                        // not reachable...
                    }
                }

                if (color != null) {
                    elements.add(new String[]{color, name});
                }
            }

            ColorItemRenderer renderer = new ColorItemRenderer();
            colorCombo.setRenderer(renderer);
            for (Object element : elements) {
                colorCombo.addItem(element);
            }
            return !elements.isEmpty();
        }

        String title = "Error";
        String msg = String.format("Cannot find %s", valuesColorsXml);
        showMessageDialog(title, msg);
        return false;
    }

    @NotNull
    private HashMap<String, String> parseColorsXml(VirtualFile colorsXml) {
        HashMap<String, String> map = new LinkedHashMap<String, String>();
        try {
            NodeList colors = getColorNodes(colorsXml.getInputStream());
            makeColorMap(colors, map);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return map;
    }

    private void makeColorMap(NodeList colors, HashMap<String, String> map) {
        for (int i = 0; i < colors.getLength(); i++) {
            Element node = (Element) colors.item(i);
            String nodeName = node.getNodeName();
            if ("color".equals(nodeName) || "item".equals(nodeName)) {
                String name = node.getAttribute("name");
                String color = node.getTextContent();
                if (name != null && color != null && !map.containsKey(name)) {
                    map.put(name, color);
                }
            }
        }
    }

    private NodeList getColorNodes(InputStream stream) throws Exception {
        XPath xPath = XPathFactory.newInstance().newXPath();
        String expression = "//item[@type=\"color\"]|//color";
        XPathExpression compile = xPath.compile(expression);
        DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = f.newDocumentBuilder();
        Document doc = builder.parse(stream);
        return (NodeList) compile.evaluate(doc, XPathConstants.NODESET);
    }

    @Nullable
    @Override
    protected JComponent createCenterPanel() {
        return contentPane;
    }

    private String getColorName(JComboBox combo) {
        Object colorItem = combo.getSelectedItem();
        try {
            if (colorItem instanceof Object[]) {
                return "@color/" + ((Object[]) (colorItem))[1];
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    @Override
    protected void doOKAction() {
        String f = filenameText.getText();
        final String outputFileName = (f.endsWith(".xml") ? f : f + ".xml").trim();
        final String color = getColorName(colorCombo);

        if (!valid(outputFileName, color)) {
            String title = "Invalidation";
            String msg = "color start with `@color/`";
            showMessageDialog(title, msg);
            return;
        }

        if (exists(outputFileName)) {
            String title = "Cannot create files";
            String msg = String.format(Locale.US,
                    "`%s` already exists", outputFileName);
            showMessageDialog(title, msg);
            return;
        }

        Application app = ApplicationManager.getApplication();
        app.runWriteAction(new Runnable() {
            @Override
            public void run() {
                try {
                    createDrawable(outputFileName);
                    createDrawableV21(outputFileName, color);
                } catch (Exception e) {
                    e.printStackTrace();
                    String title = "Cannot create files";
                    String msg = String.format(Locale.US,
                            "Oops! Something goes wrong! >w<");
                    showMessageDialog(title, msg);
                }
            }
        });
        super.doOKAction();
    }

    private boolean valid(String filename, String color) {
        if (filename.isEmpty() || ".xml".equals(filename))
            return false;

        String regex = "^@color/.+";
        return color.matches(regex);
    }

    private boolean exists(String filename) {
        String[] dirs = new String[]{drawableDir, drawableV21Dir};
        for (String d : dirs) {
            VirtualFile f = resDir.findChild(d);
            if (f != null && f.isDirectory()) {
                VirtualFile dest = f.findChild(filename);
                if (dest != null && dest.exists()) {
                    return true;
                }
            }
        }

        return false;
    }

    private void createDrawable(String filename) throws Exception {
        VirtualFile child = resDir.findChild(drawableDir);
        if (child == null) {
            child = resDir.createChildDirectory(null, drawableDir);
        }

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
        item.setAttribute("android:drawable", "@drawable/" + targetImage);
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

    private void createDrawableV21(String outputFilename,
                                   String color) throws Exception {
        VirtualFile child = resDir.findChild(drawableV21Dir);
        if (child == null) {
            child = resDir.createChildDirectory(null, drawableV21Dir);
        }

        VirtualFile newXmlFile = child.findChild(outputFilename);
        if (newXmlFile != null && newXmlFile.exists()) {
            newXmlFile.delete(null);
        }
        newXmlFile = child.createChildData(null, outputFilename);

        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();

        Document doc = builder.newDocument();
        Element root = doc.createElement("ripple");
        root.setAttributeNS(nsUri, "xmlns:android", androidUri);
        root.setAttribute("android:color", color);
        doc.appendChild(root);

        Element item = doc.createElement("item");
        item.setAttribute("android:drawable", "@drawable/" + targetImage);
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

    private void showMessageDialog(String title, String message) {
        Messages.showMessageDialog(
                project, message, title, Messages.getErrorIcon());
    }
}
