import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class FormTestdatas {
	public static void main(String[] args) throws NumberFormatException, IOException {
		FileWriter fw = new FileWriter("testdatas/testdatas9.txt");
		List<Sensor> sensors = new ArrayList<Sensor>();
		for (int i = 0; i < 10000; i++) {
			Random rd=new Random();
			double x = rd.nextDouble()*100;
			double y = rd.nextDouble()*100;
			Sensor sensor = new Sensor(x,y);
			if (!sensors.contains(sensor)) {
				sensors.add(sensor);
				fw.write(x+" "+y+"\r\n");
			}else {
				System.out.println(x+" "+y+" reduplicate");
				i--;
			}
		}
		fw.close();
	}
}

