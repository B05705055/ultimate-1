//@ ltl invariant positive: <>AP(x <= 10);

int x = 0;
void increaseX(int n){
    if (n == 0)
        return;
    x++;
    // @ assert (x > 10);
    increaseX(n-1);
}

int main(){
    // @ assert (x <= 10);
    increaseX(10);
}