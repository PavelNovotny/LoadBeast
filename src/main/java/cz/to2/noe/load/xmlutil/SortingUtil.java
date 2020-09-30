package cz.to2.noe.load.xmlutil;

/**
 * Created by pavelnovotny on 28.09.20.
 */

import javax.xml.transform.*;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;

public class SortingUtil {

    /**
     * sortuje pomoci xslt zpravy odchazejici z noe. Napravuje ruzne zprehazen itemy, parametry,...
     * @param xmlContent
     * @return
     * @throws Exception
     */
    public static String sortNoeXml(String xmlContent) throws Exception {
        String xslt = readFilefromClasspathAsText(SortingUtil.class, "templates/sorting.xslt");
        TransformerFactory f = TransformerFactory.newInstance();
        Transformer t = f.newTransformer(new StreamSource(new StringReader(xslt)));
        Source source = new StreamSource(new StringReader(xmlContent));
        StringWriter resultStringWriter = new StringWriter();
        Result result = new StreamResult(resultStringWriter);
        t.transform(source, result);
        return resultStringWriter.toString();
    }

    public static String readFilefromClasspathAsText(Class cls, String path) throws IOException {
        ClassLoader classLoader = cls.getClassLoader();
        InputStream in = classLoader.getResourceAsStream(path);
        StringBuffer sb = new StringBuffer();
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new InputStreamReader(in));
            String line = bufferedReader.readLine();
            while (line != null) {
                sb.append(line).append("\n");
                line = bufferedReader.readLine();
            }
            in.close();
        } finally {
            try {
                if (bufferedReader != null) in.close();
            } catch (IOException e) {
            }
        }
        return sb.toString();
    }



}

