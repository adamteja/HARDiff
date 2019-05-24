package com.adamteja.har;

public class HARMain {
    public static void main(String[] args){
        System.out.println("HAR Diff");

        HarUtil sourceHar = new HarUtil(args[0],args[1]);
        sourceHar.compareHars();

    }

}
