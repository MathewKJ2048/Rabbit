import java.nio.file.Files;
import java.nio.file.Paths;

public class Main
{
    public static void main(String[] args)
    {
        //args[0] = filename
        //args[1] = filename for output
        if(args.length!=2)
        {
            System.out.println("Error : incorrect arguments, default arguments used");
            args = new String[2];
            args[0] = "grammar.sdt";
            args[1] = "Parser.java";
        }
        try
        {
            String classname = args[1].substring(0,args[1].indexOf('.'));
            Files.writeString(Paths.get(args[1]),RDP.get_RDP(Files.readString(Paths.get(args[0])),classname));
            System.out.println("code generated and stored in "+classname+".java");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}