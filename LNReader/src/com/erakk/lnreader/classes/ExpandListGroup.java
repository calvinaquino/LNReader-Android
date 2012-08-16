package com.erakk.lnreader.classes;
 
import java.util.ArrayList;
 
public class ExpandListGroup {
  
    private String Name;
    private ArrayList<ExpandListChild> Items;
     
    public String getName() {
        return Name;
    }
    public void setName(String name) {
        this.Name = name;
    }
    public ArrayList<ExpandListChild> getItems() {
        return Items;
    }
    public void setItems(ArrayList<ExpandListChild> Items) {
        this.Items = Items;
    }
    
}

