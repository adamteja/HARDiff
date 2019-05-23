package com.adamteja.har;

public class HARMain {
    public static void main(String[] args){
        System.out.println("HAR Main");

        HarUtil sourceHar = new HarUtil("src/main/resources/www.williams-sonoma.com.har", "src/main/resources/aktest-www.williams-sonoma.com.har" );
        sourceHar.compareHars();
//        sourceHar.printEntries();

    }

}
