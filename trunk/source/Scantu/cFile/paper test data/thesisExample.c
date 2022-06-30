//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(y == 10));
int x = 0;
int y = 1;

int main(){
	while(1){
		x = 3;
		y = 1;
		while(x>0){
			x--;
			if(x<=1){
			y = 0;
			}
		}	
	}
}



