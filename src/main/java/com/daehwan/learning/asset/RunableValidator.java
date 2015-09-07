package com.daehwan.learning.asset;

import javax.jcr.Node;

public interface RunableValidator {
	//void run();
	void runByNode(Node node);
	boolean isValidNode(Node node);
}
