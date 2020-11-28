package myMiniGUIGenarator;

import org.w3c.dom.*;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;

public class DOMUIParser {
	private Element root;
	private JPanel app;
	private  JFrame f;


//TODO add constructor and main
//e.g. $dparser = new org.apache.xerces.parsers.DOMParser();
// 	   $document = new File(fileName);
	public DOMUIParser(){}

	/**
	 * main method to build the ui
	 * @param doc the XML doc
	 */
	protected void buildUI(Document doc){
		root = doc.getDocumentElement();
		app = new JPanel();
		processNode(root, app);
		f = new JFrame();
		f.setTitle(getTitle());
		f.getContentPane().add(app);
		f.pack();
		f.setVisible(true);
	}

	private String getTitle() {
		NodeList children = root.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if(children.item(i).getNodeName().equals("title"))
				return children.item(i).getTextContent();
		}
		return null;
	}

	private String getLabel(Node wchild) {
		NodeList children = wchild.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			if(children.item(i).getNodeName().equals("text"))
				return children.item(i).getTextContent();
		}
		return null;
	}

	/**
	 * processes a layout node of the XML file
	 * @param element the layout elementnode
	 * @return the layout as a JPanel
	 */
	private JPanel processLayout(Element element){
		NamedNodeMap n = element.getAttributes();
		GridLayout layout = null;
		try{
			layout = new GridLayout(Integer.parseInt(n.item(2).getNodeValue()), Integer.parseInt(n.item(0).getNodeValue()));
		} catch (Exception e){
			System.err.println(e.getMessage());
		}

		if(layout != null){
			JPanel newpanel = new JPanel();
			newpanel.setLayout(layout);
			NodeList nl = element.getChildNodes();
			if(nl.getLength()>0) {
				for (int i = 0; i < nl.getLength(); i++) {
					processNode(nl.item(i), newpanel);
				}
			}
			return newpanel;
		}
		return null;
	}

	/***
	 * processes a node of the XML tree
	 * @param n the node to proces
	 * @param container the container for the node to be processed in
	 */
	private void processNode(Node n, JComponent container){
		if(n.getNodeType()==Node.ELEMENT_NODE)
			if(n.getNodeName().equals("group")){
				container.add(processLayout((Element)n));
				return;
			}
			else if(processWidgetElement((Element)n, container)) return;
		NodeList nl = n.getChildNodes();
		if(nl.getLength()>0)
			for(int i=0; i<nl.getLength(); i++)
				processNode(nl.item(i), container);
	}

	/**
	 * method to split the string of a method on the last dot
	 * @param methodstring the string to split
	 * @return the splitted string
	 */
	private String[] split_methodstring(String methodstring){
		int p = methodstring.lastIndexOf(".");
		String classname = methodstring.substring(0, p);
		String methodname = methodstring.substring(p + 1);
		String[] names = new String[2];
		names[0] = classname;
		names[1] = methodname;
		return names;
	}

	/***
	 * checks if a method given is a systemIO method, cause it can cause problems for the program
	 * @param methodname the name of the method
	 * @param classname the name of the class
	 * @return
	 */
	private boolean is_systemIOMethod(String methodname, String classname){
		if (classname.equals("System.out") || classname.equals("System.in") || classname.equals("System.err")) {
			try {
				Class printstreamclass = Class.forName("java.io.PrintStream");
				Method m = printstreamclass.getMethod(methodname);
				return true;
			} catch (Exception ex) {
				return false;
			}
		}
		return false;
	}

	/***
	 * checks if a method given by the XML file is a valid method
	 * @param methodname the name of the method
	 * @return
	 */
	private boolean is_valid_method(String methodname){
		String[] names = split_methodstring(methodname);
		String classname = names[0];
		methodname = names[1];
		try{
			Class c = Class.forName(classname);
			Method method = c.getMethod(methodname);
			if(!Modifier.isStatic(method.getModifiers())){
				try{
					Constructor ct = c.getConstructor(null);
				} catch (NoSuchMethodException e){
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			return is_systemIOMethod(methodname, classname);
		}
	}

	/**
	 * class that helps to determine if type is a primitive
	 * @param paramTypeString name of the type
	 * @return
	 */
	private Class<?> getPrimitiveTypeClass(String paramTypeString) {
		switch (paramTypeString) {
			case "boolean":
				return boolean.class;
			case "byte":
				return byte.class;
			case "short":
				return short.class;
			case "int":
				return int.class;
			case "long":
				return long.class;
			case "float":
				return float.class;
			case "double":
				return double.class;
			case "char":
				return char.class;
			case "void":
				return void.class;
			default:
				return null;
		}
	}

	/***
	 * method to retrieve the parametertypes of a node of the XML tree representing a actionListener
	 * @param actionnode the node of the actionlistener
	 * @return
	 * @throws InvalidXMLException
	 * @throws ClassNotFoundException
	 */
	private Class[] get_paramtypes(Node actionnode) throws InvalidXMLException, ClassNotFoundException{
		ArrayList<Class> typelist = new ArrayList<Class>();
		NodeList list = actionnode.getChildNodes();
		for(int i = 0; i < list.getLength() - 1; ++i){
			if (list.item(i).getNodeName().equals("param")){
				if(list.item(i).getChildNodes().item(1).getNodeName().equals("type")){
					Class c = getPrimitiveTypeClass(list.item(i).getChildNodes().item(1).getTextContent());
					if (c == null){
						c = Class.forName(list.item(i).getChildNodes().item(1).getTextContent());
					}
					typelist.add(c);
				}else {
					throw new InvalidXMLException();
				}
			}
		}
		Class[] types = new Class[typelist.size()];
		int i = 0;
		for(Class c : typelist){
			types[i] = c;
			i++;
		}
		return types;
	}

	/**
	 * method to convert a string value to a primitive class value if the class is primitive
	 * @param clazz the class to be converted to
	 * @param value the string value of the argument
	 * @return
	 */
	public static Object toObject( Class clazz, String value ) {
		if( boolean.class == clazz || Boolean.class == clazz) return Boolean.parseBoolean( value );
		if( byte.class == clazz || Byte.class == clazz) return Byte.parseByte( value );
		if( short.class == clazz || Short.class == clazz) return Short.parseShort( value );
		if( int.class == clazz || Integer.class == clazz) return Integer.parseInt( value );
		if( long.class == clazz || Long.class == clazz) return Long.parseLong( value );
		if( float.class == clazz || Float.class == clazz) return Float.parseFloat( value );
		if( double.class == clazz || Double.class == clazz) return Double.parseDouble( value );
		return null;
	}

	/**
	 * method to retrieve the parametervalues of a node of the XML tree representing a actionListener
	 * @param actionnode the node to get the arguments from
	 * @param argumenttypes the type the arguments have to be
	 * @return
	 * @throws InvalidXMLException
	 */
	private Object[] get_parameters(Node actionnode, Class[] argumenttypes) throws InvalidXMLException{
		ArrayList<Object> paramlist = new ArrayList<Object>();
		NodeList list = actionnode.getChildNodes();
		for(int i = 0; i < list.getLength(); ++i){
			if(list.item(i).getNodeName().equals("param")){
				if(list.item(i).getChildNodes().item(3).getNodeName().equals("value")){
					String value = list.item(i).getChildNodes().item(3).getTextContent();
					Object param = toObject(argumenttypes[paramlist.size()], value);
					if (param == null){
						param = value;
					}
					paramlist.add(param);
				} else {
					throw new InvalidXMLException();
				}
			}
		}
		return paramlist.toArray();
	}

	/**
	 * method to make an actionlistener after all data is read from XML
	 * @param componentAsButton the button object to add an actionlistener to
	 * @param c the class of the method from the actionlistener
	 * @param m the method to be called by the actionlistener
	 * @param argumentvalues the values to pass to the method
	 */
	private void make_actionlistener(JButton componentAsButton, Class c, Method m, Object[] argumentvalues){
		componentAsButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if(Modifier.isStatic(m.getModifiers())){
					try{
						m.invoke(argumentvalues);
					} catch (Exception ex){
						System.err.println(ex.getMessage());
					}
				} else {
					try{
						if(c.getName().equals("java.io.PrintStream")){
							PrintStream p = System.out;
							m.invoke(p, argumentvalues);
						} else{
							Object o = c.newInstance();
							m.invoke(o, argumentvalues);
						}
					} catch (Exception ex){
						System.err.println(ex.getMessage());
					}
				}
			}
		});
	}

	/***
	 * method to add an action to a jcomponent
	 * @param action the actionnode
	 * @param comp the component
	 * @throws InvalidXMLException
	 * @throws NoSuchMethodException
	 */
	private void add_action(Node action, Component comp) throws InvalidXMLException, NoSuchMethodException{
		NodeList list = action.getChildNodes();
		if(list.getLength() > 0 && list.item(1).getNodeName() == "method"){
			if(is_valid_method(list.item(1).getTextContent())){
				String[] names = split_methodstring(list.item(1).getTextContent());
				String classname = names[0];
				String methodname = names[1];
				try{
					Class[] argumenttypes = get_paramtypes(action);
					Object[] argumentvalues = get_parameters(action, argumenttypes);
					try{
						Class c = Class.forName(classname);
						Method m = c.getMethod(methodname, argumenttypes);
						JButton componentAsButton = (JButton)comp;
						make_actionlistener(componentAsButton, c, m, argumentvalues);
					}catch (ClassNotFoundException e){
						if(is_systemIOMethod(methodname, classname)){
							Class c = Class.forName("java.io.PrintStream");
							Method m = c.getMethod(methodname, argumenttypes);
							JButton componentAsButton = (JButton)comp;
							make_actionlistener(componentAsButton, c, m, argumentvalues);
						} else {
							System.err.println("classname of argument of method " + methodname + " not valid in XLM file");
							return;
						}
					}
				} catch (ClassNotFoundException e){
					System.err.println("the class for argument given in XML file was invalid");
				}
			}
		}
	}

	/**
	 * method to process al children of a widget
	 * @param children the children of the widget
	 * @param parent the widget
	 * @return
	 */
	private Component processWidgetChildren(NodeList children, Component parent){
		if (children.getLength() > 0){
			boolean label_added = false;
			JPanel labeledparent = new JPanel();
			labeledparent.setLayout(new BoxLayout(labeledparent, BoxLayout.Y_AXIS));
			labeledparent.add(parent);
			for(int i = 0; i < children.getLength(); i++){
				if(children.item(i).getNodeName().equals("text")){
					label_added = true;
					JLabel label = new JLabel(children.item(i).getTextContent());
					labeledparent.add(label);
				} else if (children.item(i).getNodeName().equals("actie")){
					try{
						add_action(children.item(i), parent);
					} catch (Throwable e){
						System.err.println(e.getMessage());
					}

				}
			}
			if(label_added){
				return labeledparent;
			} else {
				return parent;
			}
		} else {
			return parent;
		}
	}

	/* Processes a widget element, adds it to the container 
	 * @param e				an element describing a widget 
	 * @param container		the container to which the widget should be added
	 * @return 				true if the descendants of e are processed by this method
	 * 						false otherwise
	 * @pre	e != null
	 * @pre e.getChildNodes() != null
	 * @pre e.getChildNodes().getLength() > 1
	 * @pre container != null
	 * */
	private boolean processWidgetElement(Element element, JComponent container) {
		Class cl;
		Constructor ct;
		try {
			cl = Class.forName(element.getNodeName());
			ArrayList<Class> paramtypes = new ArrayList<Class>();
			ArrayList<Object> params = new ArrayList<Object>();

			NamedNodeMap n = element.getAttributes();
			for(int i = 0; i < n.getLength(); ++i){
				Attr at = (Attr)n.item(i);
				String value = at.getValue();
				try{
					int val = Integer.parseInt(value);
					paramtypes.add(int.class);
					params.add(val);
				} catch (NumberFormatException e){
					paramtypes.add(Class.forName("java.lang.String"));
					params.add(value);
				}
			}
			Class[] argtypes = new Class[paramtypes.size()];
			for(int i = 0; i < paramtypes.size(); i++){
				argtypes[i] = (Class)paramtypes.get(i);
			}

			ct = cl.getConstructor(argtypes);

			Object[] arguments = params.toArray();

			Component comp = (Component)ct.newInstance(arguments);

			comp = processWidgetChildren(element.getChildNodes(), comp);

			container.add(comp);


		} catch (ClassNotFoundException e) {
			return false;
			// TODO: handle exception
		} catch (NoSuchMethodException e) {
			System.err.println("there was no constructor for the arguments and class given");
			return false;
		} catch (Exception e){
			System.err.println(e.getMessage());
		}

		if (element == null || container == null) 
			return false;
		else if (element.getChildNodes() == null) 
			return false;
		else if (element.getChildNodes().getLength() < 1)
			return false;
		else {
			return true;
		}
	}
	
	private GridLayout MakeGroup(Element element){
		GridLayout gridLayout = new GridLayout();
		String rows = element.getAttribute("rows");
		String columns = element.getAttribute("columns");
		return gridLayout;
}


}