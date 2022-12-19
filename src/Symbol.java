import java.util.*;

public class Symbol
{
    int u_id; // unique id for each rule
    String name;
    String type;
    boolean is_terminal;
    List<Rule> r = new ArrayList<>();
    public Symbol(String s)
    {
        this.name = s;
        this.is_terminal=false;
        this.type = null;
        this.u_id=0;
    }
    public Symbol()//returns epsilon by default
    {
        this.name = "";
        this.is_terminal=true;
        this.type = null;
        this.u_id=0;
    }
    public boolean is_epsilon()
    {
        return this.name.equals("")&&this.is_terminal;
    }
    public void add_rule(Rule r)
    {
        this.r.add(r);
        r.head = this;
        r.id = u_id;
        u_id++;
    }
}
