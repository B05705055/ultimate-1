//#Unsafe
#include <stdlib.h>
int plus (int inputN){
        int temp = 0;
        for (int a = 0; a < inputN; a++){
                temp = temp + a;
        }
        return temp;
}
int main() {
        int aaa = 5;
        int bbb = plus(aaa);
        return 0;
}
