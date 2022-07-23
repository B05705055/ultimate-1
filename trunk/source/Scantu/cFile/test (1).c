//@ ltl invariant positive: <>AP(x[0] > 10);

int x[2] = {0};
void increaseX(int n){
    if (n == 0)
        return;
    x[0]++;
    // @ assert (x > 10);
    increaseX(n-1);
}

int main(){
    // @ assert (x <= 10);
    increaseX(10);
}