import java.util.*;
public class Rule
{
    List<Symbol> r;
    Symbol head = null;
    int id;
    String code = null;
    public Rule()
    {
        this.r = new ArrayList<>();
    }
    public void add_symbol(Symbol s)
    {
        this.r.add(s);
    }
    public void add_code(String code)
    {
        this.code = this.code==null?code:this.code+code;
    }
    public Rule(Symbol s)
    {
        this.r = new ArrayList<>();
        this.r.add(s);
        this.code = null;
    }

}
