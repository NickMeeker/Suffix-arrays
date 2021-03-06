/*
Creation of suffix array according to Manber-Myers algorithm
The job complexity is O(N logN)

Compile this code to get .jar file
Run it from command line and pass one argument of .txt file that contains text
    to be suffix sorted.
    
    (eg. in Windows: java -jar Inf.jar text.txt
    Text file should consinsts only of letters and \n. If there is any other sign
    (eg. (space), %, 4, ") suffix array won't be ok.

Suffix array is saved to current directory in SAOutput.txt file.

REF:Suffix arrays:A new method for on-line string searches 
    Udi Manber
    Gene Myers
    May 1989
    Revised August 1991
*/


package bioinf;

import java.io.IOException;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.BufferedWriter;
import java.util.Date;
import java.util.Collections;

public class BioInf {
    
    public static void main(String[] args) {
        
        String text = "";
        Boolean success = false;
        
        try
        {
            text = ReadText(args[0]);
            success = true;
        }
        catch(Exception x)
        {
            System.out.println("Error during file reading! Please check your input file!");
            success = false;
        }
        
        if (success)
        {
            try
            {
                Doit(text);
            }
            catch(Exception e)
            {
                System.out.println("Something has gone wrong! Please check your input txt file!");
            }
        }
    }
    
    /*
    Create suffix array and sort it using Manber-Myers algorithm
    */
    static void Doit(String text)
    {
        int[] POS = new int[text.length()];         //Suffix array (output)
        int[] PRM = new int[text.length()];         //Inverse Suffix Array (PRM[POS[i]] = i)
        Boolean[] BH = new Boolean[text.length()];  //Points to the leftmost suffix of a H-bucket
        Boolean[] B2H = new Boolean[text.length()]; //Marks moved suffixes; After checking, points to the leftmost suffix in 2H-bucket
        int[] Count = new int[text.length()];       //Internal array
        int[] next = new int[text.length()];        //Internal array
        
        /*
        !!!First stage sort!!!!
        This section sets all arrays according to the first stage
        Sets all the suffixes according to their first letter (Sets POS, PRM and BH arrays)
        */
        long start =System.nanoTime();              //for time measuring
        
        java.util.HashMap<Character, Integer> Alphabet = new java.util.HashMap<Character, Integer>();
        int t = 0;
        for (char x : text.toCharArray())
            {
                if (Alphabet.keySet().contains(x))
                    {
                        int tmp=Alphabet.get(x);
                        tmp++;     
                        Alphabet.put(x, tmp);
                    }
                else
                    {
                        Alphabet.put(x, 1);
                    }
                POS[t] = -1;
                BH[t] = false;
                B2H[t] = false;
                Count[t] = 0;
                t++;
            }
        
        java.util.ArrayList<Character> Letters = new java.util.ArrayList(Alphabet.keySet());
        Collections.sort(Letters);
        
        int letterFirstposition;
        int letterOffset;
        int letterposition = 0;
        BH[0] = true;
        
        for (char x : text.toCharArray())
        {
            letterFirstposition = 0;
            for (char y : Letters)
            {
                if (x != y)
                {
                    int tmp = Alphabet.get(y);
                    letterFirstposition += tmp;
                }
                else
                {
                    break;
                }
            }
            
            letterOffset = letterFirstposition;
            BH[letterFirstposition] = true;
            while (POS[letterOffset] != -1)
            {
                letterOffset++;
            }
            
            POS[letterOffset] = letterposition;
            PRM[letterposition] = letterOffset;
            letterOffset = 0;
            letterposition++;
        }
        /*
        End of first stage sort!
        */
        
        /*
        Algorithm by H=1,2,4,8,16...H<N
        POS array and time elapsed are written in console after the sort is done
        */
        for (int h=1; h<text.length(); h=h*2)
        {
            int buckets = 0;
            for (int i=0, j; i<text.length(); i=j)
            {
                j=i+1;
                while(j<text.length() && !BH[j])
                    j++;
                next[i]=j;
                buckets++;
            }
            
            if (buckets == text.length())               //Algorithm is done after every suffix is in its own bucket
                break;
            
            for (int i=0; i<text.length(); i=next[i])   //Sets PRM array
            {
                Count[i]=0;
                for(int j=i; j<next[i]; j++)
                    PRM[POS[j]]=i;
            }
            
            Count[PRM[text.length() - h]]++;
            B2H[PRM[text.length() - h]] = true;
            
            for (int i=0; i<text.length(); i=next[i])   //Scan all buckets and update PRM, Count and B2H arrays
            {
                for (int j=i; j<next[i]; j++)           //Update arrays
                {
                    int s = POS[j] -h;
                    if (s>=0)
                    {
                        int tmp = PRM[s];
                        PRM[s] = tmp + Count[tmp]++;
                        B2H[PRM[s]] = true;
                    }
                }
                for (int j=i; j<next[i]; j++)           //Reset B2H array such that only the leftmost of them in each
                {                                       //2H-bucket is set to 1, and rest are reset to 0
                    int s = POS[j] - h;
                    if (s>=0 && B2H[PRM[s]])
                    {
                        for (int k=PRM[s] + 1; k<FindNextBH(k, BH, text.length()); k++)
                            B2H[k]=false;
                    }
                }
            }
            
            for (int i=0; i<text.length(); i++)         //Updating POS and BH arrays
            {
                POS[PRM[i]]=i;
                BH[i] |= B2H[i];
            }
        }
        
        for (int i=0; i<text.length(); i++)             //Updating PRM array
        {
            PRM[POS[i]]=i;
        }
        
        long end = System.nanoTime();
        long timeElapsed = end-start;
        
        int mb = 1024*1024;
        
        Runtime runtime = Runtime.getRuntime();
        try                                             //Saving Suffix array to file
        {
            SaveSAToFile(POS, PRM);
        }
        catch(Exception e)
        {
            System.out.println("Something went wrong during saving SA to file. Check if you are Administrator user on this PC maybe.");
        }
        
        
        
        System.out.println("\n\nSA created in " + timeElapsed/1000000 + " miliseconds");
        System.out.println("and stored to current directory in SAOutput.txt file!");
        System.out.println("Used memory "+ (runtime.totalMemory()-runtime.freeMemory())/mb + "MB or " + (runtime.totalMemory()-runtime.freeMemory())/1024 + "KB");
        System.out.println("\n\n!!!----------------NOTE----------------!!!");
        System.out.println("\nIf your input txt file has any other characters but letters, this SA is invalid and you should check your input file!");
    }
    
    
    /*
    Finds position of next true value in BH array starting from x.
    If none is found, returns last possible position in BH array
    */
    static int FindNextBH(int x, Boolean[] BH, int end)
    {
        for (int i=x; i<end; i++)
        {
            if (BH[i] == true)
                return i;
        }
        return end;
    }
    
    /*
    Reads text given as an argument and creates string that is used to create suffixes from
    */
    static String ReadText(String path) throws IOException
        {
            FileReader fr = new FileReader(path);
            BufferedReader textreader = new BufferedReader(fr);
            int numOfLines = OpenFile(path);
            String[] TextData = new String[numOfLines];
            
            for (int i=0; i < numOfLines; i++)
            {
                TextData[i] = textreader.readLine();
            }
            textreader.close();
            
            String text = "";
            for(String x: TextData)
            {
                text=text+x;
            }
            return text.toUpperCase();
            
        }
    
    /*
    Method that finds number of lines in text file to be suffix sorted
    help for ReadText method upper
    */
    static int OpenFile(String path) throws IOException
    {
        FileReader fr = new FileReader(path);
        BufferedReader temp = new BufferedReader(fr);
        
        String line;
        int numberOfLines = 0;
        
        while ((line = temp.readLine()) != null)
        {
            numberOfLines++;
        }
        return numberOfLines;
    }
    
    /*
    Method that saves POS and PRM arrays to txt file
    */
    static void SaveSAToFile(int[] POS, int[] PRM) throws IOException
    {
        BufferedWriter out = new BufferedWriter(new FileWriter("SAOutput.txt"));
        Date d = new Date();
        out.write("------------------------------------------------------------------------");
        out.newLine();
        out.write(d.toString());
        out.newLine();
        out.write("SA");
        out.newLine();
        out.write("[");
        for (int x : POS)
        {
            out.write(x + ", ");
        }
        out.write("]");
        out.newLine();
        out.newLine();
        out.newLine();
        out.write("PRM (inverse SA)");
        out.newLine();
        out.write("[");
        for (int x : PRM)
        {
            out.write(x + ", ");
        }
        out.write("]");
        out.close();
    }
     
}
