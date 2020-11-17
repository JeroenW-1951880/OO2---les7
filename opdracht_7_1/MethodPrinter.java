package com.jetbrains;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.*;

/***
 * program that prints out a members and methods of a class
 *
 * @author Jeroen Weltens
 */
public class MethodPrinter {

    /**
     * execution of the program
     */
    public static void main(String[] args) {
	    System.out.print("Class name? ");
	    BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
	    try {
            String classname = in.readLine();
            Class c = Class.forName(classname);
            System.out.println("Fields:");
            for(Field f : c.getDeclaredFields()){
                System.out.println(f.getType().getName() + " " + f.getName());
            }
            System.out.println("Methods:");
            for(Method m : c.getDeclaredMethods()){
                String argumentlist = "";
                for(Type t : m.getGenericParameterTypes()){
                    argumentlist += t.getTypeName() + ", ";
                }
                if(argumentlist != ""){
                    argumentlist = argumentlist.substring(0, argumentlist.length() - 2);
                }
                System.out.println(m.getReturnType() + " " + m.getName() + "(" + argumentlist + ");");
            }
        }catch (ClassNotFoundException e){
	        System.err.println("class not found");
        }catch (IOException e){
	        System.err.println("IO error");
        }
    }
}
