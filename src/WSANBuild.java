import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Transparency;
import java.awt.geom.Line2D;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;

/**
 * @author Xiaolei.W
 * The Class is to Build a WSAN.
 */
public class WSANBuild {
	public final static double r = 5*Math.sqrt(2)/2;
	private final static int m = 6;
	private final static double gSide = Math.sqrt(2)*r;
	private final static double uECM = 20;
	private final static double uECD = 1;
	private static List<Double> VList = new ArrayList<Double>();
	private static List<Sensor> sensors= new ArrayList<Sensor>();
	private static Grid[][] grids;
	
	/**
	 * The method is to divide all the sensors into grids.
	 * @param sensors
	 *  - the sensors is the sensors to divide
	 * @return
	 *  - the generated grids after divided
	 */
	
	public static Grid[][] DivideSensor(List<Sensor> sensors, double gSide){
		double maxX=0, maxY=0;
		for (Sensor sensor:sensors) {
			if (sensor.getX()>maxX) {
				maxX=sensor.getX();
			}
			if (sensor.getY()>maxY) {
				maxY=sensor.getY();
			}
		}
		
		int xLength = (int) (maxX/gSide)+1, yLength = (int) (maxY/gSide)+1;
		Grid[][] grids = new Grid[xLength][yLength];
		for (int y = 1; y <= yLength; y++) {
			for (int x = 1; x<= xLength; x++) {
				Grid g=new Grid();
				g.setId(xLength*(y-1)+x);
				g.setX(x);g.setY(y);
				grids[x-1][y-1]=g;
				grids[x-1][y-1].setParkingPoint(new Sensor(x*gSide-0.5*gSide, y*gSide-0.5*gSide));
			}
		}
		for (Sensor s:sensors) {
			int X=(int) (s.getX()/gSide);
			int Y=(int) (s.getY()/gSide);
			grids[X][Y].addSensors(s);
		}
		return grids;
	}
	
	/**
	 * The method is to cluster the grids.
	 * @param grids
	 *  - the grids to cluster
	 * @param m
	 *  - the mumber of cluster
	 * @return
	 *  - the generated clusters after clustered
	 */
	public static List<Cluster> ClusterGrids(Grid[][] grids, int m){
		int maxId = 1;
		List<Cluster> clusters= new ArrayList<Cluster>();
		List<Grid> unclusteredGrids = new ArrayList<Grid>();
		for (int i = 0; i < grids.length; i++) {
			for (int j = 0; j < grids[i].length; j++) {
				unclusteredGrids.add(grids[i][j]);
			}
		}
		int direction = 0, border = 0;
		while(m!=1){
			System.out.println("cluster "+maxId+":");
			Cluster cluster = new Cluster();
			cluster.setId(maxId);
			List<Sensor> sensors = new ArrayList<Sensor>();
			for (int i = 0; i < unclusteredGrids.size(); i++) {
				sensors.addAll(unclusteredGrids.get(i).getSensors());
			}
			int e = sensors.size()/m;
			
			//To calculate splitX and splitY of the sensors to cluster
			double sumX = 0, sumY = 0;
			for (int i = 0; i < sensors.size(); i++) {
				sumX += sensors.get(i).getX();
				sumY += sensors.get(i).getY();
			}
			double eX = sumX/sensors.size(), eY = sumY/sensors.size();
			double splitX = 0, splitY = 0;
			for (int i = 0; i < sensors.size(); i++) {
				splitX += Math.pow(sensors.get(i).getX()-eX,2);
				splitY += Math.pow(sensors.get(i).getY()-eY,2);
			}
			splitX = splitX/sensors.size();
			splitY = splitY/sensors.size();
			
			if (splitX>=splitY) {
				direction = 1;
				Collections.sort(sensors, new Comparator<Sensor>() {
					public int compare(Sensor s1, Sensor s2) {
						return ((Double)s1.getX()).compareTo((Double)s2.getX());
						}
					}
				);
				int CLeft = 0, CRight = 0;
				double xBorder = sensors.get(e).getX();
				border =  xBorder%gSide==0?(int)(xBorder/gSide)-1:(int)(xBorder/gSide);
				for (Grid g:unclusteredGrids) {
					if (xBorder-g.getParkingPoint().getX()>0.5*gSide) {
						CLeft += g.getSize();
						CRight +=g.getSize();
						cluster.addGrid(g);
						System.out.println("  grid("+g.getX()+","+g.getY()+") ");
					}else if(xBorder-g.getParkingPoint().getX()==0.5*gSide||Math.abs(g.getParkingPoint().getX()-xBorder)<0.5*gSide){
						CRight +=g.getSize();
					}
				}
				unclusteredGrids.removeAll(cluster.getGrids());
				if(e-CLeft>=CRight-e){
					border += 1;
					for (Grid g:unclusteredGrids) {
						if(xBorder-g.getParkingPoint().getX()==0.5*gSide||Math.abs(g.getParkingPoint().getX()-xBorder)<0.5*gSide){
							cluster.addGrid(g);
							System.out.println("  grid("+g.getX()+","+g.getY()+") ");
						}
					}
				}
				unclusteredGrids.removeAll(cluster.getGrids());
			}else {
				direction = 0;
				Collections.sort(sensors, new Comparator<Sensor>() {
					public int compare(Sensor s1, Sensor s2) {
						return ((Double)s1.getY()).compareTo((Double)s2.getY());
						}
					}
				);
				int CTop = 0, CBottom = 0;
				double yBorder = sensors.get(e).getY();
				border = yBorder%gSide==0?(int)(yBorder/gSide)-1:(int)(yBorder/gSide);
				for (Grid g:unclusteredGrids) {
					if (yBorder-g.getParkingPoint().getY()>0.5*gSide) {
						CTop += g.getSize();
						CBottom +=g.getSize();
						cluster.addGrid(g);
						System.out.println("  grid("+g.getX()+","+g.getY()+") ");
					}else if(yBorder-g.getParkingPoint().getY()==0.5*gSide||Math.abs(g.getParkingPoint().getY()-yBorder)<0.5*gSide){
						CBottom +=g.getSize();
					}
				}
				unclusteredGrids.removeAll(cluster.getGrids());
				if(e-CTop>=CBottom-e){
					border += 1;
					for (Grid g:unclusteredGrids) {
						if(yBorder-g.getParkingPoint().getY()==0.5*gSide||Math.abs(g.getParkingPoint().getY()-yBorder)<0.5*gSide){
							cluster.addGrid(g);
							System.out.println("  grid("+g.getX()+","+g.getY()+") ");
						}
					}
				}
				unclusteredGrids.removeAll(cluster.getGrids());
			}
			System.out.println(border);
			cluster.setDirection(direction);
			cluster.setBorder(border);
			cluster.setEC(cluster.getSize()*uECM+cluster.getAllSensors().size()*uECD);
			clusters.add(cluster);
			maxId++;
			m--;
		}
		System.out.println("cluster "+maxId+":");
		Cluster cluster = new Cluster();
		cluster.setId(maxId);
		cluster.setDirection(direction);
		cluster.setGrids(unclusteredGrids);
		for (int i = 0; i < cluster.getSize(); i++) {
			System.out.println("  grid("+cluster.getGrids().get(i).getX()+","+cluster.getGrids().get(i).getY()+") ");
		}
		cluster.setEC(cluster.getSize()*uECM+cluster.getAllSensors().size()*uECD);
		clusters.add(cluster);
		return clusters;
	}
	
	/**
	 * The method is to rebalance the clusters.
	 * @param clusters
	 *  - the clusters to operate
	 */
	public static List<Cluster> RebalanceCluster(List<Cluster> clusters){
		// TODO Auto-generated method stub
		List<Cluster> betterClusters = new ArrayList<Cluster>();
		double E = 0; //E: the ideal energy consumption in each cluster
		double V = 0; //V: the variance of energy consumption
		int n = 1;
		for (int i = 0; i < clusters.size(); i++) {
			E += clusters.get(i).getEC();
		}
		E = E/clusters.size();
		for (int i = 0; i < clusters.size(); i++) {
			V += Math.pow(clusters.get(i).getEC()-E, 2); 
		}
		V = V/clusters.size();
		System.out.println("V="+V);
		double minV = V;
		VList.add(V);
		System.out.println("\nStart to rebalance the clusters:");
		int repeated = 0;
		while (true) {
			System.out.println("Period "+n++);
			for (int i = 0; i < clusters.size(); i++) {
				if (clusters.get(i).getEC()>E) {
					Cluster targetNeighbor = null; //the neighbor with the smallest energy consumption
					List<Grid> gn = new ArrayList<Grid>();
					double tnEc = clusters.get(i).getEC();//target neighbor energy consumption
					/**
					 * Find the neighbor with the smallest energy consumption
					 */
					for (int j = 0; j < clusters.size(); j++) {
						if (isNeighbor(clusters.get(i), clusters.get(j))&&j!=i&&clusters.get(j).getEC()<tnEc) {
							tnEc = clusters.get(j).getEC();
							targetNeighbor = clusters.get(j);
						}
					}
					if (targetNeighbor != null) {
						int direction; //1 - portrait; 0 -  landscape
						int border; //border X(Y)
						if (clusters.get(i).getId()<targetNeighbor.getId()) { 
							direction = clusters.get(i).getDirection();
							border = clusters.get(i).getBorder();
							for (int j = 0; j < clusters.get(i).getSize(); j++) {
								if (direction==0&&(clusters.get(i).getGrids().get(j).getY()==border||clusters.get(i).getGrids().get(j).getY()==border+1)) {
									/**
									 * if the clusters.get(i) is upon targetNeighbor
									 */
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()
												&&targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()+1) {
											gn.add(clusters.get(i).getGrids().get(j));
											break;
										}
									}
								}else if (direction==1&&(clusters.get(i).getGrids().get(j).getX()==border||clusters.get(i).getGrids().get(j).getX()==border+1)){						
									/**
									 * if the clusters.get(i) is on the left of targetNeighbor
									 */
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()
												&&targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()+1) {
											gn.add(clusters.get(i).getGrids().get(j));
											break;
										}
									}
								}
							}
						}else {
							direction = targetNeighbor.getDirection();
							border = targetNeighbor.getBorder();
							for (int j = 0; j < clusters.get(i).getSize(); j++) {
								if (direction==0&&(clusters.get(i).getGrids().get(j).getY()==border+1||clusters.get(i).getGrids().get(j).getY()==border)) {
									/**
									 * if the clusters.get(i) is under targetNeighbor
									 */
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()
												&&targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()-1) {
											gn.add(clusters.get(i).getGrids().get(j));
											break;
										}
									}
								}else if (direction==1&&(clusters.get(i).getGrids().get(j).getX()==border+1||clusters.get(i).getGrids().get(j).getX()==border)){	
									/**
									 * if the clusters.get(i) is on the right of targetNeighbor
									 */
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()
												&&targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()-1) {
											gn.add(clusters.get(i).getGrids().get(j));
											break;
										}
									}
								}
							}
							
						}
						
						/**
						 * sort the grids in gn
						 */
						if (direction==1) {
							Collections.sort(gn, new Comparator<Grid>() {
									public int compare(Grid g1, Grid g2) {
										int flag = ((Integer)g1.getX()).compareTo((Integer)g2.getX());
										if(flag==0){
											return  ((Integer)g1.getY()).compareTo((Integer)g2.getY());
										}else{
											return flag;
										}  
									}});
						}else {
							Collections.sort(gn, new Comparator<Grid>() {
								public int compare(Grid g1, Grid g2) {
									int flag = ((Integer)g1.getY()).compareTo((Integer)g2.getY());
									if(flag==0){
										return  ((Integer)g1.getX()).compareTo((Integer)g2.getX());
									}else{
										return flag;
									}  
								}});
						}
						
						for (int j = 0; j < gn.size(); j++) {
							int index = clusters.get(i).getId()<targetNeighbor.getId()?(gn.size()-j-1):j;
							tnEc = tnEc + uECD*gn.get(index).getSize() + uECM;
							double ec = clusters.get(i).getEC() - uECD*gn.get(index).getSize() - uECM;
							clusters.get(i).getGrids().remove(gn.get(index));
							clusters.get(i).setEC(ec);
							targetNeighbor.getGrids().add(gn.get(index));
							targetNeighbor.setEC(tnEc);
							clusters.set(clusters.indexOf(targetNeighbor), targetNeighbor);
							System.out.println("grid("+gn.get(index).getX()+","+gn.get(index).getY()+"): "
									+clusters.get(i).getId()+" -> "+targetNeighbor.getId());
							if (tnEc>=ec) {
								System.out.println("ok");
								break;
							}
						}
					}
				}else if (clusters.get(i).getEC()<E) {
					Cluster targetNeighbor = null;
					List<Grid> gn = new ArrayList<Grid>();
					double tnEc = clusters.get(i).getEC();//target neighbor energy consumption

					for (int j = 0; j < clusters.size(); j++) {
						if (isNeighbor(clusters.get(i), clusters.get(j))&&j!=i&&clusters.get(j).getEC()>tnEc) {
							tnEc = clusters.get(j).getEC();
							targetNeighbor = clusters.get(j);
						}
					}
					if (targetNeighbor != null) {
						int direction, border;
						if (clusters.get(i).getId()<targetNeighbor.getId()) {
							direction = clusters.get(i).getDirection();
							border = clusters.get(i).getBorder();
							for (int j = 0; j < clusters.get(i).getSize(); j++) {
								if (direction==0&&(clusters.get(i).getGrids().get(j).getY()==border||clusters.get(i).getGrids().get(j).getY()==border-1)) {
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()
												&&targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()+1) {
											gn.add(targetNeighbor.getGrids().get(k));
											break;
										}
									}
								}else if (direction==1&&(clusters.get(i).getGrids().get(j).getX()==border||clusters.get(i).getGrids().get(j).getX()==border-1)){
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()
												&&targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()+1) {
											gn.add(targetNeighbor.getGrids().get(k));
											break;
										}
									}
								}
							}
						}else {
							direction = targetNeighbor.getDirection();
							border = targetNeighbor.getBorder();
							for (int j = 0; j < clusters.get(i).getSize(); j++) {
								if (direction==0&&(clusters.get(i).getGrids().get(j).getY()==border+1||clusters.get(i).getGrids().get(j).getY()==border+2)) {
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()
												&&targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()-1) {
											gn.add(targetNeighbor.getGrids().get(k));
											break;
										}
									}
								}else if (direction==1&&(clusters.get(i).getGrids().get(j).getX()==border+1||clusters.get(i).getGrids().get(j).getX()==border+2)){
									for (int k = 0; k < targetNeighbor.getSize(); k++) {
										if (targetNeighbor.getGrids().get(k).getY()==clusters.get(i).getGrids().get(j).getY()
												&&targetNeighbor.getGrids().get(k).getX()==clusters.get(i).getGrids().get(j).getX()-1) {
											gn.add(targetNeighbor.getGrids().get(k));
											break;
										}
									}
								}
							}
							
						}
						if (direction==1) {
							Collections.sort(gn, new Comparator<Grid>() {
									public int compare(Grid g1, Grid g2) {
										int flag = ((Integer)g1.getX()).compareTo((Integer)g2.getX());
										if(flag==0){
											return  ((Integer)g1.getY()).compareTo((Integer)g2.getY());
										}else{
											return flag;
										}  
									}});
						}else {
							Collections.sort(gn, new Comparator<Grid>() {
								public int compare(Grid g1, Grid g2) {
									int flag = ((Integer)g1.getY()).compareTo((Integer)g2.getY());
									if(flag==0){
										return  ((Integer)g1.getX()).compareTo((Integer)g2.getX());
									}else{
										return flag;
									}
								}});
						}
						for (int j = 0; j < gn.size(); j++) {
							int index = clusters.get(i).getId()<targetNeighbor.getId()?j:(gn.size()-j-1);
							tnEc = tnEc - uECD*gn.get(index).getSize() - uECM;
							double ec = clusters.get(i).getEC() + uECD*gn.get(index).getSize() + uECM;
							targetNeighbor.getGrids().remove(gn.get(index));
							targetNeighbor.setEC(ec);
							clusters.set(clusters.indexOf(targetNeighbor), targetNeighbor);
							clusters.get(i).getGrids().add(gn.get(index));
							clusters.get(i).setEC(tnEc);
							System.out.println("grid("+gn.get(index).getX()+","+gn.get(index).getY()+"): "
									+clusters.get(i).getId()+" <- "+targetNeighbor.getId());
							if (tnEc<=ec) {
								System.out.println("ok");
								break;
							}
						}
					}
				}
			}
			E = 0; V = 0;
			for (int i = 0; i < clusters.size(); i++) {
				E += clusters.get(i).getEC();
			}
			E = E/clusters.size();
			for (int i = 0; i < clusters.size(); i++) {
				V += Math.pow(clusters.get(i).getEC()-E, 2); 
			}
			V = V/clusters.size();
			System.out.println("V="+V);
			
			/**
			 * Find the best rebalence result.
			 */
			if(V<minV){
				minV = V;
				betterClusters =  new ArrayList<Cluster>();
				for (int i = 0; i < clusters.size(); i++) {
					Cluster cluster = clusters.get(i);
					betterClusters.add(new Cluster(cluster.getId(),cluster.getDirection(),cluster.getBorder(),new ArrayList<Grid>(cluster.getGrids()),cluster.getEC()));
				}
			}
			
			/**
			 * Decide whether to break the loop.
			 */
			if (VList.contains(V)) {
				repeated ++;
				if (repeated == 10) {
					break;
				}
			}
			VList.add(V);
		}
		System.out.println("minV = "+minV);
		return betterClusters;
	}
	
	/**
	 * Decide whether cluster 1 and cluster 2 are neighbors.
	 * @param c1
	 *  - cluster 1
	 * @param c2
	 *  - cluster 2
	 * @return
	 *  - true: cluster 1 and cluster 2 are neighbors
	 *  - false: cluster 1 and cluster 2 are not neighbors
	 */
	public static Boolean isNeighbor(Cluster c1, Cluster c2){
		for (int i = 0; i < c1.getSize(); i++) {
			for (int j = 0; j < c2.getSize(); j++) {
				if ((c1.getGrids().get(i).getX()==c2.getGrids().get(j).getX()&&Math.abs(c1.getGrids().get(i).getY()-c2.getGrids().get(j).getY())==1)||(c1.getGrids().get(i).getY()==c2.getGrids().get(j).getY()&&Math.abs(c1.getGrids().get(i).getX()-c2.getGrids().get(j).getX())==1)) {
					return true;
				}
			}
		}
		return false;
	}
	
	/**
	 * Create an image of the generated clusters.
	 * @param clusters
	 *  - the resource of clusters
	 * @param url
	 *  - a pathname string
	 * @throws IOException 
	 *  - if an error occurs during writing.
	 */
	public static void printCluster(List<Cluster> clusters, String url) throws IOException{
		int[][] drawgrids = new int[grids.length][grids[0].length];
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = 0; j < clusters.get(i).getSize(); j++) {
				Grid grid = clusters.get(i).getGrids().get(j);
				drawgrids[grid.getX()-1][grid.getY()-1] = i+1;
			}
		}
		List<Sensor> drawsensors = new ArrayList<Sensor>(sensors);
		int t = 70; //magnification
		int thin = t/14;
		int medium = 2*t/7;
		int thick = 5*t/7;
		BufferedImage image=new BufferedImage((int)(drawgrids.length*gSide*t), (int)(drawgrids[0].length*gSide*t), BufferedImage.TYPE_INT_RGB);
		Graphics2D g2 =  image.createGraphics();
		image = g2.getDeviceConfiguration().createCompatibleImage((int)(drawgrids.length*gSide*t), (int)(drawgrids[0].length*gSide*t), Transparency.TRANSLUCENT); 
		g2.dispose();
		g2 = image.createGraphics(); 
		g2.setPaint(Color.BLACK);//set stroke color
		
		/**
		 * Draw sensors.
		 */
		for (int i = 0; i < drawsensors.size(); i++) {
			int x = (int)(drawsensors.get(i).getX()*t);
			int y = (int)(drawsensors.get(i).getY()*t);
			g2.fillOval(x, y, medium, medium);
		}
		
		/**
		 * Draw Grids.
		 */
		for (int i = 0; i < drawgrids.length; i++) {
			for (int j = 0; j < drawgrids[i].length; j++) {
				int x = (int)(i*gSide*t);
				int y = (int)(j*gSide*t);
				int w = (int)(gSide*t);
				int h = (int)(gSide*t);
				Rectangle2D rect=new Rectangle2D.Double(x,y,w,h);
				g2.setStroke(new BasicStroke(thin, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
				g2.draw(rect);
			}
		}
		
		/**
		 * Draw parting line。
		 */
		for (int i = 0; i < drawgrids.length; i++) {
			for (int j = 0; j < drawgrids[i].length; j++) {
				int x = (int)(i*gSide*t);
				int y = (int)(j*gSide*t);
				if (i+1<drawgrids.length&&drawgrids[i][j]!=drawgrids[i+1][j]) {
					Line2D line = new Line2D.Double((int)((i+1)*gSide*t), y, (int)((i+1)*gSide*t), (int)((j+1)*gSide*t));
					g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
					g2.draw(line);
				}
				if (j+1<drawgrids[i].length&&drawgrids[i][j]!=drawgrids[i][j+1]) {
					Line2D line = new Line2D.Double(x, (int)((j+1)*gSide*t), (int)((i+1)*gSide*t), (int)((j+1)*gSide*t));
					g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
					g2.draw(line);
				}
			}
		}
		
		Rectangle2D rectframe=new Rectangle2D.Double(0,0,drawgrids.length*gSide*t,drawgrids[0].length*gSide*t);
		g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
		g2.draw(rectframe);
		
		ImageIO.write(image, "png", new File(url));
		System.out.println("Image generated successfully!");
	}
	
	/**
	 * Show the generated clusters with JFrame.
	 * @param clusters
	 * - the resource of clusters
	 */
	public static void showCluster(List<Cluster> clusters){
		int[][] print = new int[grids.length][grids[0].length];
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = 0; j < clusters.get(i).getSize(); j++) {
				Grid grid = clusters.get(i).getGrids().get(j);
				print[grid.getX()-1][grid.getY()-1] = i+1;
			}
		}
		final List<Sensor> drawsensors = sensors;
		final int[][] drawgrids = print;
		final int t = 7; //magnification
		final int thin = t/7;
		final int medium = 2*t/7;
		final int thick = 3*t/7;
		
		JFrame jf=new JFrame();
		JComponent jc =new JComponent() {
			private static final long serialVersionUID = -7249806437019568554L;

			public void paintComponent(Graphics g){
				Graphics2D g2=(Graphics2D)g;
				g2.setPaint(Color.BLACK);//set stroke color
				
				for (int i = 0; i < drawsensors.size(); i++) {
					int x = (int)(drawsensors.get(i).getX()*t);
					int y = (int)(drawsensors.get(i).getY()*t);
					g2.fillOval(x, y, medium, medium);
				}
				for (int i = 0; i < drawgrids.length; i++) {
					for (int j = 0; j < drawgrids[i].length; j++) {
						int x = (int)(i*gSide*t);
						int y = (int)(j*gSide*t);
						int w = (int)(gSide*t);
						int h = (int)(gSide*t);
						Rectangle2D rect=new Rectangle2D.Double(x,y,w,h);
						g2.setStroke(new BasicStroke(thin, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
						g2.draw(rect);
					}
				}
				
				for (int i = 0; i < drawgrids.length; i++) {
					for (int j = 0; j < drawgrids[i].length; j++) {
						int x = (int)(i*gSide*t);
						int y = (int)(j*gSide*t);
						if (i+1<drawgrids.length&&drawgrids[i][j]!=drawgrids[i+1][j]) {
							Line2D line = new Line2D.Double((int)((i+1)*gSide*t), y, (int)((i+1)*gSide*t), (int)((j+1)*gSide*t));
							g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
							g2.draw(line);
						}
						if (j+1<drawgrids[i].length&&drawgrids[i][j]!=drawgrids[i][j+1]) {
							Line2D line = new Line2D.Double(x, (int)((j+1)*gSide*t), (int)((i+1)*gSide*t), (int)((j+1)*gSide*t));
							g2.setStroke(new BasicStroke(thick, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
							g2.draw(line);
						}
					}
				}
				
				Rectangle2D rectframe=new Rectangle2D.Double(0,0,drawgrids.length*gSide*t,drawgrids[0].length*gSide*t);
				g2.setStroke(new BasicStroke(3f, BasicStroke.CAP_SQUARE, BasicStroke.JOIN_ROUND)); 
				g2.draw(rectframe);
			}
		};

		jf.setTitle("Draw WSAN");
		jf.setSize((int)(grids.length*gSide*t+17),(int)(grids[0].length*gSide*t+40));
		jf.add(jc);
		jf.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		jf.setVisible(true);
	}
	
	/**
	 * Build WSAN.
	 * @param args
	 * @throws IOException
	 *  - if an error occurs during writing.
	 */
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		File file = new File("testdatas/testdatas8.txt");
		InputStreamReader read = new InputStreamReader(new FileInputStream(file));   
		BufferedReader in = new BufferedReader(read);
		String s;
		while((s = in.readLine()) != null) {
			Sensor sensor = new Sensor(Double.parseDouble(s.split(" ")[0]), 0.75*Double.parseDouble(s.split(" ")[1]));
			sensors.add(sensor);
		}
		read.close(); 
		//sensors = sensors.subList(0, 1999);
		grids = DivideSensor(sensors, gSide);
		List<Cluster> clusters = ClusterGrids(grids, m);
		showCluster(clusters);
		printCluster(clusters,"img/oriImg8.png");
		clusters = RebalanceCluster(clusters);
		showCluster(clusters);
		printCluster(clusters,"img/rstImg8-10.png");

		/*FileWriter fw = new FileWriter("results/result8 -gside100.txt");
		for (int i = 0; i < VList.size(); i++) {
			fw.write(VList.get(i)+"\r\n");
		}
		fw.close();*/

		/*int[][] print = new int[grids.length][grids[0].length];
		for (int i = 0; i < clusters.size(); i++) {
			for (int j = 0; j < clusters.get(i).getSize(); j++) {
				Grid grid = clusters.get(i).getGrids().get(j);
				print[grid.getX()-1][grid.getY()-1] = i+1;
			}
		}
		
		for (int i = 0; i < print[0].length; i++) {
			for (int j = 0; j < print.length; j++) {
				System.out.print(print[j][i]+" ");
			}
			System.out.println();
		}
		System.out.println();*/
		
		/*for (int n = 1; n <= 9; n++) {
			sensors = new ArrayList<Sensor>();
			VList = new ArrayList<Double>();
			File file = new File("testdatas/testdatas"+n+".txt");
			InputStreamReader read = new InputStreamReader(new FileInputStream(file));   
			BufferedReader in = new BufferedReader(read);
			String s;
			while((s = in.readLine()) != null) {
				Sensor sensor = new Sensor(Double.parseDouble(s.split(" ")[0]), 0.75*Double.parseDouble(s.split(" ")[1]));
				sensors.add(sensor);
			}
			read.close(); 

			grids = DivideSensor(sensors, gSide);
			List<Cluster> clusters = ClusterGrids(grids, m);
			//printCluster(clusters,"img2/originalImg"+n+".png");
			clusters = RebalanceCluster(clusters);
			//printCluster(clusters,"img2/resultImg"+n+".png");
			
			System.out.println();
			FileWriter fw = new FileWriter("results/result"+n+".txt");
			for (int i = 0; i < VList.size(); i++) {
				fw.write(VList.get(i)+"\r\n");
			}
			fw.close();
		}*/
	}
}

/**
 * @author Xiaolei.W
 *
 */
class Cluster{
	private int id;
	private int direction;
	private int border;
	private List<Grid> grids = new ArrayList<Grid>();
	private double EC;

	public Cluster(int id, int direction, int border, List<Grid> grids,
			double eC) {
		super();
		this.id = id;
		this.direction = direction;
		this.border = border;
		this.grids = grids;
		EC = eC;
	}

	public Cluster() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void addGrid(Grid grid) {
		this.grids.add(grid);
	}
	
	public void addGrids(List<Grid> grids) {
		this.grids.addAll(grids);
	}
	
	/**
	 * The method is to get the number of grid in a cluster.
	 * @return
	 */
	public int getSize() {
		return this.grids.size();
	}
	
	/**
	 * The method is to get all the sensors in a cluster.
	 * @return
	 */
	public List<Sensor> getAllSensors() {
		List<Sensor> sensors =  new ArrayList<Sensor>();
		for (int i = 0; i < this.grids.size(); i++) {
			sensors.addAll(this.grids.get(i).getSensors());
		}
		return sensors;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public List<Grid> getGrids() {
		return grids;
	}

	public void setGrids(List<Grid> grids) {
		this.grids = grids;
	}

	public double getEC() {
		return EC;
	}

	public void setEC(double eC) {
		EC = eC;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}
	
	public int getBorder() {
		return border;
	}

	public void setBorder(int border) {
		this.border = border;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Cluster other = (Cluster) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "SubRegion[id=" + id + "]";
	}

}

/**
 * @author Xiaolei.W
 *
 */
class Grid{
	private int id, X, Y;
	private List<Sensor> sensors = new ArrayList<Sensor>();
	private Sensor parkingPoint= new Sensor();
	
	public Grid(int id, List<Sensor> sensors) {
		super();
		this.id = id;
		this.sensors = sensors;
	}

	public Grid() {
		super();
		// TODO Auto-generated constructor stub
	}

	public void addSensors(Sensor sensor) {
		this.sensors.add(sensor);
	}
	
	/**
	 * 
	 * This method is to get the number of sensor in this grid。
	 * @return
	 */
	public int getSize() {
		return this.sensors.size();
	}
	
	public int getId() {
		return id;
	}
	public void setId(int id) {
		this.id = id;
	}
	
	public int getX() {
		return X;
	}

	public void setX(int x) {
		X = x;
	}

	public int getY() {
		return Y;
	}

	public void setY(int y) {
		Y = y;
	}

	public void setSensors(List<Sensor> sensors) {
		this.sensors = sensors;
	}
	public List<Sensor> getSensors() {
		return sensors;
	}

	public Sensor getParkingPoint() {
		return parkingPoint;
	}

	public void setParkingPoint(Sensor sensor) {
		this.parkingPoint = sensor;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + id;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Grid other = (Grid) obj;
		if (id != other.id)
			return false;
		return true;
	}

	@Override
	public String toString() {
		return "Grid " + id ;
	}
	
}


/**
 * @author Xiaolei.W
 *
 */
class Sensor{
	private double x, y;	
	public Sensor(double x, double y){
		this.x = x;
		this.y = y;
	}

	public Sensor() {
		super();
		// TODO Auto-generated constructor stub
	}

	public double distance(Sensor node){
		double sqrX = (this.x - node.x) * (this.x - node.x), 
				sqrY = (this.y - node.y) * (this.y - node.y);
		return Math.sqrt(1.0 * (sqrX + sqrY));
	}
	
	public double getX() {
		return x;
	}
	public void setX(double x) {
		this.x = x;
	}
	public double getY() {
		return y;
	}
	public void setY(double y) {
		this.y = y;
	}

	@Override
	public String toString() {
		return "("+x + ", " + y + ")";
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Sensor other = (Sensor) obj;
		if (Double.doubleToLongBits(x) != Double.doubleToLongBits(other.x))
			return false;
		if (Double.doubleToLongBits(y) != Double.doubleToLongBits(other.y))
			return false;
		return true;
	}

}
