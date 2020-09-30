package cz.to2.noe.load;

import cz.to2.noe.load.file.ReadLoad;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Properties;

/**
 * Created by pavelnovotny on 07.09.20.
 */
public class PrepareLoadFile {
    private Properties properties;
    private static Logger logger = LoggerFactory.getLogger(PrepareLoadFile.class);


    public PrepareLoadFile(String propertyFileName) {
        loadProperties(propertyFileName);
    }

    public static void main(String[] args) {
        String propertyFile = "beast-load.properties";
        if (args.length >0) {
            propertyFile = args[0];
        }
        PrepareLoadFile prepareLoad = new PrepareLoadFile(propertyFile);
        try {
            prepareLoad.createLoadFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void createLoadFile() throws Exception {
        logger.info("Start");
        ReadLoad readLoad = new ReadLoad();
        readLoad.setProperties(this.properties);
        readLoad.create();
        logger.info("End");
    }


    private void loadProperties(String propertyFileName){
        File propsFile=new File(propertyFileName);
        try {
            this.properties = new Properties();
            this.properties.load(new FileInputStream(propsFile));
        } catch (Throwable t) {
            throw new RuntimeException("Nelze nahrat properties :"+propsFile.getAbsolutePath(),t);
        } finally {
        }
    }


}
