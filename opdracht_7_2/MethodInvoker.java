import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.InputMismatchException;


/**
 * class to test the invoking of methods via reflection
 *
 * @author Jeroen Weltens
 */
public class MethodInvoker {

    /***
     * Method that does command-line communication with the user to get the method he wants to invoke
     * @return the name of the method and class, respectively in an array of 2
     * @throws IOException
     */
    private static String[] get_MethodAndClassName() throws IOException{
        System.out.print("methodname? ");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        String methodname = in.readLine();
        String[] stringparts = methodname.split("\\."); //split the name at the last "."
        String classname = "";
        for(int i = 0; i < stringparts.length - 1; ++i){
            classname += stringparts[i] + ".";
        }
        methodname = stringparts[stringparts.length - 1];
        classname = classname.substring(0, classname.length() - 1);
        String[] names = new String[2];
        names[0] = classname;
        names[1] = methodname;
        return names;
    }

    /***
     * Method to convert the input of the user to a Method object
     * @param classname the name of the class that contains the method
     * @param methodname the name of the method chosen
     * @return the found method Object in the class
     * @throws ClassNotFoundException
     * @throws NoSuchMethodException
     * @throws IllegalMethodException when the method is not static and the constructor of the class has parameters
     */
    private static ArrayList<Method> input_to_method(String classname, String methodname) throws ClassNotFoundException, NoSuchMethodException, IllegalMethodException {
        Class c = Class.forName(classname);
        Method[] allMethods = c.getDeclaredMethods();
        ArrayList<Method> m = new ArrayList<Method>();
        boolean all_methods_static = true;
        for (Method method : allMethods) {
            if (method.getName().equals(methodname) && all_params_acceptable(method)) {
                m.add(method);
                if (!Modifier.isStatic(method.getModifiers())) {
                    all_methods_static = false;
                }
            }
        }
        if(m.isEmpty()){ //no methods found
            throw new NoSuchMethodException();
        }
        if(!validate_methodchoice(all_methods_static, c)){
            throw new IllegalMethodException();
        }

        return m;
    }

    /***
     * this method checks if all parameters of the method are being able to be initialized by an int or String
     * @param m the chosen method
     * @return boolean representing the awnser
     */
    private static boolean all_params_acceptable(Method m){
        /*for(Type t : m.getParameterTypes()){
            if (!String.class.getClass().isAssignableFrom(t.getClass()) && !Integer.class.getClass().isAssignableFrom(t.getClass())){
                return false;
            }
        }*/
        return true;
    }

    /***
     * method to check if the methods chosen are static, or come out of a static class or out of a class with a default constructor
     * @param all_methods_static boolean to represent if all methods chosen are static
     * @param c the Class of the methods chosen
     * @return boolean to represent if the methodchoise is valid
     */
    private static boolean validate_methodchoice(boolean all_methods_static, Class c){
        if(!all_methods_static) {
            if (!Modifier.isStatic(c.getModifiers())) {
                boolean empty_constructor_found = false;
                Constructor[] constructors = c.getConstructors();
                for (Constructor con: constructors){
                    TypeVariable[] t = con.getTypeParameters();
                    if (con.getTypeParameters().length == 0){
                        empty_constructor_found = true;
                    }
                }
                if(!empty_constructor_found){
                  return false;
                }
            }
        }
        return true;
    }

    /***
     * Method that communicates with the user through the console how many parameters he wants to select
     * @param m the method selected
     * @return the number of parameters the user has chosen
     * @throws IOException
     */
    private static int ask_numOfParams(ArrayList<Method> m) throws IOException, NumberFormatException{
        ArrayList<Integer> numberoptions = new ArrayList<Integer>();
        for(Method method : m){
            if(!numberoptions.contains(method.getParameterCount())){
                numberoptions.add(method.getParameterCount());
            }
        }
        System.out.print("possible num of parameters: ");
        String numberstring = "";
        for(int i : numberoptions){
            numberstring += " " + i + ",";
        }
        numberstring = numberstring.substring(0, numberstring.length() - 1);
        System.out.print(numberstring + "? ");
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        try {
            int num = Integer.parseInt(in.readLine());
            if(numberoptions.contains(num)){
                return num;
            } else {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException e){ //when the user enters no number
            System.out.println("Number entered was mo option, defaulted to " + numberoptions.get(0));
            return numberoptions.get(0);
        }
    }

    /**
     * gets the first method in the selected list with the asked number of parameters
     * @param methods the selected methods
     * @param paramnum the number of parameters chosen
     * @return the method with the asked number of parameters
     * @throws NoSuchMethodException when no method is found
     */
    private static Method get_asked_method(ArrayList<Method> methods, int paramnum) throws NoSuchMethodException{
        for(Method m : methods){
            if(m.getParameterCount() == paramnum){
                return m;
            }
        }
        throw new NoSuchMethodException();
    }

    /***
     * Method that communicates with the user through the console to enter the parameters
     * @param m the selected method
     * @return the parameterlist made by the user
     * @throws IOException
     * @throws InputMismatchException when the input is unexpected for the type of parameter
     */
    private static ArrayList<Object> paraminput(Method m) throws IOException, InputMismatchException{
        ArrayList<Object> paramlist = new ArrayList<Object>();
        int counter = 0;
        BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
        for(Type t : m.getParameterTypes()){
            counter++;
            System.out.print("parameter " + counter + " (" + t.getTypeName() + "): ");
            String input = in.readLine();
             //string-like input
            if((input.startsWith("'") && input.endsWith("'")) || (input.charAt(0) == '"' && input.charAt(input.length() - 1) == '"')){
                paramlist.add(input.substring(1, input.length() - 1).toCharArray());
            } else{//integer-like input
                try{
                    int inp = Integer.parseInt(input);
                    paramlist.add(inp);
                }catch (NumberFormatException e){ //when no number is entered
                    throw new InputMismatchException();
                }
            }
        }
        return paramlist;
    }

    /**
     * method to invoke the method chosen
     * @param argumentlist the argumentlist chosen for the method
     * @param m the selcted method
     * @param c the class of the selected method
     */
    private static void invoke_method(ArrayList<Object> argumentlist, Method m, Class c){
        Object[] args = new Object[argumentlist.size()];
        args = argumentlist.toArray();

        if(/*Modifier.isStatic(m.getModifiers())*/false){
            try{ //if the method is static
                System.out.println(m.invoke(args));
                System.out.println("invoke succesfull");
            } catch (Exception e){
                System.out.println("invoking method failed");
            }
        } else {//if the method is not static
            try{
                Object o = c.newInstance(); //first create object to call the method from
                try {
                    System.out.println(m.invoke(o, args));
                    System.out.println("invoke succesfull");
                } catch (Exception e){
                    System.out.println("invoking method failed");
                }
            } catch (Exception e){
                System.out.println("creating class object failed");
            }
        }
    }

    /***
     * main method to run the application through
     * @param sysargs null
     */
    public static void main(String[] sysargs){
        try{
            String[] names = get_MethodAndClassName();
            ArrayList<Method> selected = input_to_method(names[0], names[1]);
            int paramnums = ask_numOfParams(selected);
            Method method = get_asked_method(selected, paramnums);
            ArrayList<Object> parameterlist = paraminput(method);
            Class c = Class.forName(names[0]);
            invoke_method(parameterlist, method, c);
        } catch (IOException e){
            System.err.println(e.getMessage());
        } catch (ClassNotFoundException e){
            System.err.println(e.getMessage());
        } catch (NoSuchMethodException e){
            System.err.println(e.getMessage());
        } catch (IllegalMethodException e){
            System.err.println(e.getMessage());
        }catch (InputMismatchException e){
            System.err.println(e.getMessage());
        }
    }
}


class IllegalMethodException extends RuntimeException{
    public static String message = "Method must be static or class must be static or class must have parameterless constructor.";
    @Override
    public String getMessage() {
        return message;
    }
}