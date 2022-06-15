//#Unsafe
#include <stdlib.h>
int main() {
        int aaa[2];
        aaa[0] = 5;
        aaa[1] = 3;
//@ asdasdsa
        int x = aaa[0];
        return 0;
}
