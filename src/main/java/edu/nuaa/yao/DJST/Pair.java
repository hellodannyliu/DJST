/*
 * Copyright (C) 2007 by
 * 
 * 	Xuan-Hieu Phan
 *	hieuxuan@ecei.tohoku.ac.jp or pxhieu@gmail.com
 * 	Graduate School of Information Sciences
 * 	Tohoku University
 * 
 *  Cam-Tu Nguyen
 *  ncamtu@gmail.com
 *  College of Technology
 *  Vietnam National University, Hanoi
 *
 * JGibbsLDA is a free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation; either version 2 of the License,
 * or (at your option) any later version.
 *
 * JGibbsLDA is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JGibbsLDA; if not, write to the Free Software Foundation,
 * Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */

package edu.nuaa.yao.DJST;

public class Pair<T1,T2> implements Comparable<Pair<T1,T2>> {
	public T1 first;
	public Comparable<T2> second;
	public static boolean naturalOrder = false;
	
	public Pair(T1 k, Comparable<T2> v){
		first = k;
		second = v;		
	}
	
	public Pair(T1 k, Comparable<T2> v, boolean naturalOrder){
		first = k;
		second = v;
		Pair.naturalOrder = naturalOrder; 
	}
	
	@SuppressWarnings("unchecked")
	public int compareTo(Pair<T1,T2> p){
		if (naturalOrder)
			return this.second.compareTo((T2) p.second);
		else return -this.second.compareTo( (T2) p.second);
	}
}

