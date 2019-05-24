package com.adamteja.har;

import de.sstoehr.harreader.HarReader;
import de.sstoehr.harreader.HarReaderException;
import de.sstoehr.harreader.model.Har;
import de.sstoehr.harreader.model.HarEntry;
import de.sstoehr.harreader.model.HarHeader;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

public class HarUtil {

    private Har sourceHar;
    private Har targetHar;

    List<HarEntry> sourceEntries;
    List<HarEntry> targetEntries;

    public HarUtil(){

    }

    public HarUtil(String harfile1, String harfile2){
        loadHars(harfile1, harfile2);

        sourceEntries = sourceHar.getLog().getEntries();
        String primaryDomain = sourceHar.getLog().getPages().get(0).getTitle();
        sourceEntries = applyPrimaryDomainFilter(sourceEntries, primaryDomain);
        sourceEntries = applyBlacklist(sourceEntries);

        targetEntries = targetHar.getLog().getEntries();
        primaryDomain = targetHar.getLog().getPages().get(0).getTitle();
        targetEntries = applyPrimaryDomainFilter(targetEntries, primaryDomain);
        targetEntries = applyBlacklist(targetEntries);

    }

    public void compareHars(){


        List<String[]> sourceUniques = getUniques(sourceEntries);
        List<String[]> targetUniques = getUniques(targetEntries);

        List<String[]>[] rv = diffUniquesLists( sourceUniques, targetUniques);

        System.out.println("compareHars "+rv[0].size()+","+rv[1].size());

        //log to Reporter only if differences are found
        if( rv[0].size() > 0 || rv[1].size() > 0 ){
            //source output
            System.out.println("==== Items in Source, but not in Target ("+ rv[0].size() +")====");
            for(int i = 0; i < rv[0].size(); i++) {
                String sUrl = rv[0].get(i)[0];
                String sMimeType = rv[0].get(i)[1];
                String sStatus = rv[0].get(i)[2];
                String sCount = rv[0].get(i)[3];
                System.out.println("URL  |  MIMETYPE  |  STATUS  |  COUNT ");
                System.out.println(sUrl + " | " + sMimeType + " | " + sStatus + " | " + sCount +"");


                String[][] vars = {{"url",sUrl},{"MIMETYPE",sMimeType},{"STATUS",sStatus}};
                List<HarEntry> s = searchEntriesListForTag(sourceEntries, vars);

                System.out.println("     URLs that match pattern ("+ s.size()+") ---- ");
                for(int j=0; j < s.size(); j++){
                    System.out.println( "     "+j+":"+s.get(j).getRequest().getUrl().toString() +"");
                }
                System.out.println("     -----");
            }

            //target output
            System.out.println("==== Items in Target, but not in Source ("+ rv[1].size() +")====");
            for(int i = 0; i < rv[1].size(); i++) {
                String sUrl = rv[1].get(i)[0];
                String sMimeType = rv[1].get(i)[1];
                String sStatus = rv[1].get(i)[2];
                String sCount = rv[1].get(i)[3];
                System.out.println("URL  |  MIMETYPE  |  STATUS  |  COUNT ");
                System.out.println(sUrl + " | " + sMimeType + " | " + sStatus + " | " + sCount +"");


                String[][] vars = {{"url",sUrl},{"MIMETYPE",sMimeType},{"STATUS",sStatus}};
                List<HarEntry> s = searchEntriesListForTag(targetEntries, vars);
                System.out.println("     URLs that match pattern ("+ s.size()+") ---- ");
                for(int j=0; j < s.size(); j++){
                    System.out.println( "     "+j+":"+s.get(j).getRequest().getUrl().toString() +"");
                }
                System.out.println("     -----");



            }

        }else{
            System.out.println("passed ");
        }


    }

    public void loadHars(String harfile1, String harfile2){
        try {
            HarReader harReader = new HarReader();
            sourceHar = harReader.readFromFile(new File(harfile1));
            targetHar = harReader.readFromFile(new File(harfile2));

        }catch(HarReaderException e){
            System.out.println(e.getMessage());
        }
    }

    public void printHarEntries(List<HarEntry> entries){

        for(int i=0; i<entries.size(); i++){

            String url = entries.get(i).getRequest().getUrl().toString();
            int status = entries.get(i).getResponse().getStatus();
            String mimetype = getContentType(entries.get(i).getResponse().getHeaders());

            System.out.println(i+": "+ url +
                    ", "+ status +
                    ", "+ mimetype);

        }

    }

    public void printEntries(){
        System.out.println("--Source-------------");
        printHarEntries(sourceEntries);
        System.out.println("--Target-------------");
        printHarEntries(targetEntries);
    }

    public String getContentType(List<HarHeader> harHeader){

        List<String> responseTypes = new ArrayList<>();
        responseTypes.add("text/");
        responseTypes.add("image/");
        responseTypes.add("text/");
        responseTypes.add("font/");
        responseTypes.add("application/");
        //responseTypes.add("302"); // 302/redirect, get report-uri?

        //System.out.println("++++");
        for(int j=0; j<harHeader.size(); j++){

            for(int i=0; i<responseTypes.size(); i++){
                if(harHeader.get(j).getValue().contains(responseTypes.get(i))){
                    return harHeader.get(j).getValue();
                }
//                else{
//                    System.out.println(harHeader.get(j).getValue() + "-- no match to "+responseTypes.get(i));
//                }
            }
        }

        return "";

    }

    public List<HarEntry> applyBlacklist( List<HarEntry> entries){

        List<HarEntry> rv = new ArrayList<>();
        List<String> whitelist = getListFromFile("whitelist_hosts.txt");
        List<String> blacklist = getListFromFile("blacklist_hosts.txt");


        for(int i=0; i<entries.size(); i++) {
            String strurl = entries.get(i).getRequest().getUrl();
            try {
                URL url = new URL(strurl);
                String rr_hostname = url.getHost();

                if (whitelist.contains(rr_hostname)) {
                    // check the whitelist, if on the whitelist, save it
                    rv.add(entries.get(i));
                } else if (blacklist.contains(rr_hostname)) {
                    // if not on whitelist, but on blacklist, skip it
                } else {
                    // all others are saved
                    rv.add(entries.get(i));
                }
            }catch(MalformedURLException e){

            }
        }

        return rv;
    }

    public List<HarEntry> applyPrimaryDomainFilter(List<HarEntry> entries, String primaryDomain){
        List<HarEntry> rv = new ArrayList<>();

        for(int i=0; i<entries.size(); i++) {
            String strurl = entries.get(i).getRequest().getUrl();

            if(!strurl.contains(primaryDomain)){
                rv.add(entries.get(i));
            }
        }
        return rv;
    };

    private List<String> getListFromFile(String filename ){
        List<String> list = new ArrayList<>();

//        ClassLoader classLoader = getClass().getClassLoader();
//        File file = new File(classLoader.getResource(filename).getFile());

        File file = new File(filename);

        try{
            Scanner sc = new Scanner(file);

            while(sc.hasNext()){
                String item = sc.nextLine();
                list.add(item);
            }
        }catch (IOException e){
            e.printStackTrace();
        }
        return list;
    }

    private List<String[]> getUniques(List<HarEntry> entries){

        List<String[]> uniques = new ArrayList<>();

        //loop through source and count uniques
        for( int i = 0; i < entries.size(); i++ ){
            HarEntry source = entries.get(i);
            try{
                URL sourceURL = new URL (entries.get(i).getRequest().getUrl());
                String sourceBaseURL = sourceURL.getHost();
                String sourceMimeType = getContentType(entries.get(i).getResponse().getHeaders());
                String sourceStatus = String.valueOf(entries.get(i).getResponse().getStatus());

                // create a list of unique url/mime/status entries
                //      if exists, add to counter
                //      else if new, add and set counter to 1
                boolean match = false;
                for(int j = 0; j < uniques.size(); j++) {

                    String uUrl = uniques.get(j)[0];
                    String uMimeType = uniques.get(j)[1];
                    String uStatus = uniques.get(j)[2];

                    if (sourceBaseURL.equals(uUrl) &&
                            sourceMimeType.equals(uMimeType) &&
                            sourceStatus.equals(uStatus)) {
                        match = true;
                        //increment counter
                        int counter = Integer.parseInt(uniques.get(j)[3]);
                        counter++;
                        String[] a = {sourceBaseURL, sourceMimeType, sourceStatus, Integer.toString(counter)};
                        uniques.set(j, a);
//                        System.out.println("match");
                        match = true;
                        j = uniques.size();  // at match, bail on this loop

                    }
                }
                if(match==false){
                    String[] a = {sourceBaseURL,sourceMimeType,sourceStatus,"1"};
                    uniques.add(a);
//                    System.out.println("no match adding");
                }



            }catch(MalformedURLException e){

            }


        }

        return uniques;
    }

    private List<String[]>[] diffUniquesLists( List<String[]> sourceUniques, List<String[]> targetUniques){
        List<String[]>[] rv = new List[2];
        List<String[]> sourceUniquesDiff = new ArrayList<>();
        List<String[]> targetUniquesDiff = new ArrayList<>();


        System.out.println("===================");
        System.out.println("Initial sizes Source: "+ sourceUniques.size() +" Target: "+targetUniques.size());

        //find source uniques
        for(int i = 0; i < sourceUniques.size(); i++) {
            String sUrl = sourceUniques.get(i)[0];
            String sMimeType = sourceUniques.get(i)[1];
            String sStatus = sourceUniques.get(i)[2];
            String sCount = sourceUniques.get(i)[3];

            boolean match = false;
            for (int j = 0; j < targetUniques.size(); j++) {
                String tUrl = targetUniques.get(j)[0];
                String tMimeType = targetUniques.get(j)[1];
                String tStatus = targetUniques.get(j)[2];
                String tCount = targetUniques.get(j)[3];

                if( sUrl.equals(tUrl)&&
                        sMimeType.equals(tMimeType) &&
                        sStatus.equals(tStatus) &&
                        sCount.equals(tCount)
                ){
                    match = true;
                    j = targetUniques.size();
                }
            }
            if (match == false) {
                sourceUniquesDiff.add(sourceUniques.get(i));
            }
        }
        //source output
        System.out.println("==== Source ("+ sourceUniquesDiff.size() +")====");
        for(int i = 0; i < sourceUniquesDiff.size(); i++) {
            String sUrl = sourceUniquesDiff.get(i)[0];
            String sMimeType = sourceUniquesDiff.get(i)[1];
            String sStatus = sourceUniquesDiff.get(i)[2];
            String sCount = sourceUniquesDiff.get(i)[3];

            System.out.println(sUrl + "  " + sMimeType + "  " + sStatus + "  " + sCount);
        }

        //find target uniques
        for(int i = 0; i < targetUniques.size(); i++) {
            String sUrl = targetUniques.get(i)[0];
            String sMimeType = targetUniques.get(i)[1];
            String sStatus = targetUniques.get(i)[2];
            String sCount = targetUniques.get(i)[3];

            boolean match = false;
            for (int j = 0; j < sourceUniques.size(); j++) {
                String tUrl = sourceUniques.get(j)[0];
                String tMimeType = sourceUniques.get(j)[1];
                String tStatus = sourceUniques.get(j)[2];
                String tCount = sourceUniques.get(j)[3];

                if( sUrl.equals(tUrl)&&
                        sMimeType.equals(tMimeType) &&
                        sStatus.equals(tStatus) &&
                        sCount.equals(tCount)
                ){
                    match = true;
                    j = sourceUniques.size();
                }
            }
            if (match == false) {
                targetUniquesDiff.add(targetUniques.get(i));
            }
        }
        //target output
        System.out.println("==== Target ("+ targetUniquesDiff.size() +")====");
        for(int i = 0; i < targetUniquesDiff.size(); i++) {
            String sUrl = targetUniquesDiff.get(i)[0];
            String sMimeType = targetUniquesDiff.get(i)[1];
            String sStatus = targetUniquesDiff.get(i)[2];
            String sCount = targetUniquesDiff.get(i)[3];

            System.out.println(sUrl + "  " + sMimeType + "  " + sStatus + "  " + sCount);

        }

        rv[0]=sourceUniquesDiff;
        rv[1]=targetUniquesDiff;
        return rv;

    }

    private List<HarEntry> searchEntriesListForTag(List<HarEntry> EntriesList, String[][] keyValues ){
        List<HarEntry> EntriesListRV = new ArrayList<>();

        String matchUrl = keyValues[0][1];
        int matchstatus = Integer.parseInt(keyValues[2][1]);
        String matchMimetype = keyValues[1][1];

        for(int i=0; i<EntriesList.size(); i++){

            String url = EntriesList.get(i).getRequest().getUrl().toString();
            int status = EntriesList.get(i).getResponse().getStatus();
            String mimetype = getContentType(EntriesList.get(i).getResponse().getHeaders());

            if( url.contains(matchUrl) &&
                status == matchstatus &&
                mimetype.equals(matchMimetype)){
                EntriesListRV.add(EntriesList.get(i));
            }

        }
        return EntriesListRV;

    }
}
