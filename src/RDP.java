import java.util.ArrayList;
import java.util.List;

public class RDP
{

	public static List<Symbol> parse(List<String> tokens) throws Exception // returns a list of non-terminals in grammar
	{
		List<Symbol> heads = new ArrayList<>(); // a list of heads of productions, no redundancy
		List<Symbol> heads_redundant = new ArrayList<>(); // head of particular rule, redundant
		List<Integer> head_indices = new ArrayList<>();
		for(int i=0;i<tokens.size()-1;i++)
		{
			if(tokens.get(i+1).equals("->"))
			{
				String head = tokens.get(i);
				if(head.length()==0)throw new Exception("missing head for rule");
				char d = head.charAt(0);
				if(d=='"')throw new Exception("terminal placed at head of rule");
				else if(d=='{' || d==';' || d=='|')throw new Exception("no head given for rule");
				// search if given head already exists
				Symbol s = null;
				for(Symbol l : heads)if(l.name.equals(head)){s=l;break;}
				if(s == null)
				{
					s = new Symbol(head);
					heads.add(s);
				}
				head_indices.add(i);
				heads_redundant.add(s);
			}
		}
		for(int i : head_indices)if(i!=head_indices.get(0) && !tokens.get(i-1).equals(";"))throw new Exception("missing semicolon before rule for "+tokens.get(i));
		for(int p =0;p<head_indices.size();p++)
		{
			int i=head_indices.get(p);
			try
			{
				int j=i+1;
				Rule r = new Rule();
				while(true)
				{
					j++;
					String tkn = tokens.get(j);
					Symbol head = heads_redundant.get(p);
					if(tkn.equals("|"))
					{
						if(r.r.size()!=0)head.add_rule(r);
						r = new Rule();
					}
					else if(tkn.equals(";"))
					{
						if(r.r.size()!=0)head.add_rule(r);
						break;
					}
					else if(tkn.charAt(0)=='{')
					{
						r.add_code(tkn);
					}
					else if(tkn.charAt(0)=='"')
					{
						Symbol t = new Symbol(tkn.substring(1,tkn.length()-1));
						t.is_terminal=true;
						r.add_symbol(t);
					}
					else
					{
						//search all non-terminals, ie. heads
						Symbol x = null;
						for(Symbol nt:heads)if(nt.name.equals(tkn))x = nt;
						if(x==null)throw new Exception("error: rule missing for non-terminal "+tkn);
						r.add_symbol(x);
					}
				}
			}
			catch(Exception e) {e.printStackTrace();throw new Exception("error in rule given for "+heads.get(head_indices.get(i)).name);}
		}
		return heads;
	}
	public static void epsilon_order(Symbol s) // pushes epsilon derivations to the last
	{
		List<Integer> indices = new ArrayList<>();
		List<String> codes = new ArrayList<>(); // to consolidate codes of all epsilon derivations
		for(int i = 0; i<s.r.size();i++)
		{
			Rule r = s.r.get(i);
			if(r.r.size()==1)
			{
				Symbol t = r.r.get(0);
				if(t.is_terminal && t.name.equals(""))
				{
					indices.add(i);
					if(r.code!=null)codes.add(r.code);
				}
			}
		}
		for(int i = indices.size()-1;i>=0;i--)
		{
			s.r.remove((int)indices.get(i));
		}
		if(indices.size()!=0)
		{
			s.add_rule(new Rule(new Symbol()));
			StringBuilder cds = new StringBuilder();
			for(String c : codes)cds.append(c);
			if(cds.length()!=0)s.r.get(s.r.size()-1).code = cds.toString();
		}
	}
	public static void remove_epsilons(Rule r) // remove epsilons from between rules
	{
		if(r.r.size()==1)return;
		List<Integer> indices = new ArrayList<>();
		for(int i=0;i<r.r.size();i++)
		{
			if(r.r.get(i).is_epsilon())indices.add(i);
		}
		for(int i = indices.size()-1;i>=0;i--)
		{
			r.r.remove((int)indices.get(i));
		}
		if(indices.size()==r.r.size())r.r.add(new Symbol());
	}
	public static String code_Symbol(Symbol s)
	{
		StringBuilder st = new StringBuilder();
		st.append("\n\tboolean _").append(s.name).append("()\n\t{");
		st.append("\n\t\tint k = _pt;\n\t\t_Node x = _parse_tree;");
		for(Rule r : s.r)
		{
			st.append("\n\t\t_pt = k;\n\t\t_parse_tree = new _Node(\"").append(s.name).append("\");\n\t\t");
			for(Symbol sp : r.r)
			{
				String condition = !sp.is_terminal?"_"+sp.name+"()":"_search(\""+sp.name+"\")";
				st.append("if(").append(condition).append(")");
			}
			st.append("{").append(r.code != null ? "_parse_tree.set_code(this::"+associated_function(r)+");" : "/*null code*/").append("x.add(_parse_tree);_parse_tree=x;").append("return true;}");
		}
		st.append("\n\t\t_pt = k;\n\t\t_parse_tree = x;\n\t\treturn false;\n\t}\n");
		return st.toString();
	}
	public static String code(List<Symbol> non_terminals)
	{
		StringBuilder x = new StringBuilder();
		for(Symbol head : non_terminals)
		{
			x.append(code_Symbol(head));
			for(Rule r:head.r)if(r.code!=null)
			{
				StringBuilder init = new StringBuilder("\t\t"+head.type+" $$ = ("+head.type+")_parse_tree.$$;\n");
				StringBuilder update = new StringBuilder("\t\t_parse_tree.$$ = $$;\n");
				for(int i = 0;i<r.r.size();i++)
				{
					String type = r.r.get(i).type;
					if(type==null)continue;
					init.append("\t\t").append(type).append(" $").append((i+1)).append(" = (").append(type).append(")_parse_tree.get_$(").append(i).append(");\n");
					update.append("\t\t_parse_tree.set_$(").append(i).append(",$").append((i+1)).append(");\n");
				}
				x.append("\tvoid ")
						.append(associated_function(r))
						.append("()\n\t{\n")
						.append(init)
						.append(r.code).append("\n")
						.append(update)
						.append("\t}\n");
			}
		}
		return x.toString();
	}
	public static String wrap(String import_code, String global_code, String classname, String code, String start_name)
	{
		global_code = global_code==null?"":global_code;
		import_code = import_code==null?"":import_code;

		String imports = """
				import java.util.List;
				import java.util.ArrayList;
				""";

		String _search = """
				\tboolean _search(String terminal)
				\t{
				\t\tfor(int i=0;i<terminal.length();i++)
				\t\t{
				\t\t\tif(_pt+i>=_input.length())return false;
				\t\t\tif(_input.charAt(_pt+i)!=terminal.charAt(i))return false;
				\t\t}
				\t\t_pt+=terminal.length();
				\t\t_parse_tree.add(new _Node(terminal));
				\t\treturn true;
				\t}
				""";
		String _execute_code = """
				\tvoid _execute_code(_Node x)
				\t{
				\t\tif(x==null)return;
				\t\tfor(_Node t : x.children)_execute_code(t);
				\t\t_parse_tree = x;
				\t\tx.execute();
				\t}
				""";

		String _parse = "\tboolean _parse()\n" +
				"\t{\n" +
				"\t\t_parse_tree = new _Node(\"_START\");\n" +
				"\t\tboolean x = _"+start_name+"();\n" +
				"\t\tif(_parse_tree.children.size()!=0)_parse_tree = _parse_tree.children.get(0);\n" +
				"\t\tif(x&&(_pt==_input.length()))\n" +
				"\t\t{\n" +
				"\t\t\t_execute_code(_parse_tree);\n" +
				"\t\t\treturn true;\n" +
				"\t\t}\n" +
				"\t\treturn false;\n" +
				"\t}";

		String _Node = """
				static class _Node
				\t{
				\t\t@FunctionalInterface
				\t\tinterface Executable
				\t\t{
				\t\t\tvoid execute();
				\t\t}
				\t\tExecutable code;
				\t\tString symbol;
				\t\tList<_Node> children;
				\t\tObject $$;
				\t\tvoid add(_Node x)
				\t\t{
				\t\t\tthis.children.add(x);
				\t\t}
				\t\tObject get_$(int i)
				\t\t{
				\t\t\treturn this.children.get(i).$$;
				\t\t}
				\t\tvoid set_$(int i, Object o)
				\t\t{
				\t\t\tthis.children.get(i).$$ = o;
				\t\t}
				\t\tpublic _Node(String symbol)
				\t\t{
				\t\t\tthis.$$ = null;
				\t\t\tthis.symbol = symbol;
				\t\t\tthis.children = new ArrayList<>();
				\t\t\tthis.code=null;
				\t\t}
				\t\tpublic void set_code(Executable e)
				\t\t{
				\t\t\tthis.code = e;
				\t\t}
				\t\tpublic void execute()
				\t\t{
				\t\t\tif(code!=null)code.execute();
				\t\t}
				\t}
				""";

		String _parse_tree = """
				\tString _parse_tree()
				\t{
				\t\treturn _parse_tree_util(_parse_tree,0);
				\t}
				\tString _parse_tree_util(_Node x, int spaces)
				\t{
				\t\tStringBuilder s = new StringBuilder();
				\t\tif(x==null)return s.toString();
				\t\tString t = _SPACE.repeat(spaces);
				\t\ts.append(t).append(x.symbol).append("\\n");
				\t\tif(x.children.size()==0)return s.toString();
				\t\ts.append(t).append(_OPEN).append("\\n");
				\t\tspaces++;
				\t\tfor(_Node k : x.children)s.append(_parse_tree_util(k,spaces));
				\t\ts.append(t).append(_CLOSE).append("\\n");
				\t\treturn s.toString();
				\t}
				""";

		String _derivation = """
				\tString _derivation(boolean rightmost)
				\t{
				\t\tStringBuilder s = new StringBuilder();
				\t\tList<_Node> l = new ArrayList<>();
				\t\tl.add(_parse_tree);
				\t\twhile(true)
				\t\t{
				\t\t\ts.append(_DELIMITER);
				\t\t\tfor(_Node n : l)s.append(n.symbol).append(_DELIMITER);
				\t\t\t_Node e = null;
				\t\t\tint id=-1;
				\t\t\tint start = rightmost?0:l.size()-1;
				\t\t\tint end = l.size()-1-start;
				\t\t\tint update = rightmost?1:-1;
				\t\t\tfor(int i=start;(start-i)*(i-end)>=0;i+=update)
				\t\t\t{
				\t\t\t\tif(l.get(i).children.size()!=0){e=l.get(i);id=i;}
				\t\t\t}
				\t\t\tif(e==null)break;
				\t\t\ts.append(" ->\\n");
				\t\t\tl.remove(id);
				\t\t\tfor(int i=e.children.size()-1;i>=0;i--)l.add(id,e.children.get(i));
				\t\t}
				\t\treturn s.toString();
				\t}
				""";

		String constructor = "\tpublic "+classname+"(String in)\n" +
				"\t{\n" +
				"\t\tthis._input = in;\n\t\tthis._pt = 0;\n\t\tthis._parse_tree = null;\n" +
				"\t}\n";

		String global_variables = """
				\tString _DELIMITER = "";
				\tString _SPACE = "  ";
				\tString _OPEN = "{";
				\tString _CLOSE = "}";
				\t_Node _parse_tree;
				\tString _input;
				\tint _pt;
				""";

		return "" + imports + "\n//user-generated imports\n" + import_code +
				"\n\npublic class " + classname + "\n{\n" +
				"\t//start of user-generated global code\n" +
				global_code +
				"\t//end of user-generated global code\n" +
				_Node + global_variables + constructor + _parse_tree + _derivation + _search + _execute_code +
				"\t//autogenerated parsing logic\n" +
				_parse + code +
				"\n}";
	}
	public static List<String> tokenize(String contents) throws Exception
	{
		/*
		Rules:
		1) \t, \n, \r and space are delimiters
		2) anything enclosed in "" or {} are tokens
		3) rules for ""
			a) string is defined as open followed by close
			b) \" is not counted once inside a string since the " is escaped
			c) \\  can be used to escape a backslash inside the string
			Therefore, the closing quote must be preceded by an even number of backslashes
		4) rules for {}
			a) brackets inside strings don't count
			b) brackets inside '' don't count
		 */
		List<String> l =  new ArrayList<>();
		StringBuilder token = new StringBuilder();
		for(int i=0;i<contents.length();i++)
		{
			char d = contents.charAt(i);
			if(d==' ' || d=='\n' || d=='\t' || d=='\r')
			{
				if(token.length()!=0)
				{
					l.add(token.toString());
					token = new StringBuilder();
				}
			}
			else if(d==';' || d=='|')
			{
				l.add(""+d);
				token=new StringBuilder();
			}
			else if(d=='}')throw new Exception("error - unbalanced brackets");
			else if(d == '{')
			{
				if(token.length()!=0)l.add(token.toString());
				token = new StringBuilder();
				int stack=1;

				try{
					boolean set = false;
					while(stack!=0)
					{
						i++;
						char x = contents.charAt(i);
						if(x=='\"' || x=='\'')
						{
							int ct=0;
							while(contents.charAt(i-ct-1)=='\\')ct++;
							if(ct%2==0)set=!set;
						}
						if(x=='{' && !set)stack++;
						if(x=='}' && !set)stack--;
						token.append(x);
					}
				}catch(Exception e){throw new Exception("error: unbalanced bracket");}
				l.add('{'+token.toString());
				token = new StringBuilder();
			}
			else if(d == '"')
			{
				// complete current token
				if(token.length()!=0)l.add(token.toString());
				token = new StringBuilder();
				int ct=0;
				try {
					while(true)// keep consuming input till the next " not preceded by \ is encountered
					{
						i++;
						char x = contents.charAt(i);
						token.append(x);
						if (x == '"' && ct%2==0) break;
						if(x=='\\')ct++;
						else ct=0;
					}
				}catch (Exception e){e.printStackTrace(); throw new Exception("error - unterminated string");}
				l.add('"'+token.toString());
				token = new StringBuilder();
			}
			else
			{
				token.append(d);
			}
		}
		if(token.length()!=0)
		{
			l.add(token.toString());
		}
		return l;
	}
	public static String associated_function(Rule r)
	{
		return "_"+r.head.name+r.id;
	}
	public static String get_RDP(String input,String classname) throws Exception
	{
		List<String> tokens = RDP.tokenize(input);
		List<Symbol> l = RDP.parse(tokens);
		for(Symbol s : l)RDP.epsilon_order(s);
		for(Symbol s : l)for(Rule r : s.r)RDP.remove_epsilons(r);
		String global = null;
		String imports = null;
		String types;
		int t = 1;
		for(int i=0;i<tokens.size();i++)if(tokens.get(i).equals("->")){t=i;break;}
		try{types=tokens.get(t-2).substring(1,tokens.get(t-2).length()-1);}catch (Exception e){e.printStackTrace();throw new Exception("type section is missing");}
		try
		{
			global=tokens.get(t-3).substring(1,tokens.get(t-3).length()-1);
			imports=tokens.get(t-4).substring(1,tokens.get(t-4).length()-1);
		}catch(Exception ignored){}
		process_types(l,types);
		return RDP.wrap(imports,global,classname,RDP.code(l),l.get(0).name);
	}
	public static void process_types(List<Symbol> l,String type_section)throws Exception
	{
		String type = null;
		List<String> NT = new ArrayList<>();
		StringBuilder s = new StringBuilder();
		for(int i=0;i<type_section.length();i++)
		{
			char x = type_section.charAt(i);
			if(x==';')
			{
				for(String t : NT)
				{
					Symbol z = null;
					for(Symbol y : l)if(y.name.equals(t))z=y;
					if(z==null)throw new Exception("non-terminal "+t+" is not defined");
					else if(z.is_terminal)throw new Exception(t+" is a terminal; terminals cannot have type");
					else if(z.type!=null)throw new Exception("The type for "+t+" has already been defined as"+z.type);
					z.type = type;
				}
				NT = new ArrayList<>();
				type = null;
				s = new StringBuilder();
			}
			else if(x==' '||x=='\t'||x=='\n'||x=='\r')
			{
				if(s.length() != 0)
				{
					if(type == null)type = s.toString();
					else NT.add(s.toString());
					s = new StringBuilder();
				}
			}
			else
			{
				s.append(x);
			}
		}
		if(type!=null)throw new Exception("missing semicolon in the type section");
		for(Symbol t : l)if(!t.is_terminal && t.type==null)t.type="Object";
	}
}