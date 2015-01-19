package externalJARExamples;

import java.util.Date;
import com.terra.util.Conversor;


public class L1 {

		Date d = new Date();
		
		public void Method(){
		String s = Conversor.dateToString(d, "dd/MMMM/yyyy" );
		System.out.println(s);
	}

}

