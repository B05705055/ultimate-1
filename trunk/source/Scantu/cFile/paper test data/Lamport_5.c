//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 5));

#include <stdio.h>
#include <pthread.h>
#include <assert.h>
// #define N 2
typedef unsigned long int pthread_t;

int x = 0;
int number0, number1, number2, number3;
int entering0, entering1, entering2, entering3;

int largest(int number0, int number1, int number2, int number3)
{
    //int i;
    // Initialize maximum element 
    int max = number0;
    //for (i = 1; i < n; i++)
    if (number1 > max)
        max = number1;
    if(number2 > max)
        max = number2;
    if(number3 > max)
        max = number3;
    return max;
}



void *f0(void *_){
        //printf("Starting thread %d\n", n);
        entering0 = 1;
        number0 = 1 + largest(number0, number1, number2, number3);
        entering0 = 0;

        while(entering0){}
        while ( number0 != 0  &&
                ( number0 < number0 || ( number0 == number0 && 0 < 0) ) ){}

        while(entering1){}
        while ( number1 != 0  &&
                ( number1 < number0 || ( number1 == number0 && 1 < 0) ) ){}

        while(entering2){}
        while ( number2 != 0  &&
                ( number2 < number0 || ( number2 == number0 && 2 < 0) ) ){}

        while(entering3){}
        while ( number3 != 0  &&
                ( number3 < number0 || ( number3 == number0 && 3 < 0) ) ){}

        /* critical section */
        int y = x;
        y++;
        x = y;

        number0 = 0;

        //printf("Ending thread %d\n", n);
}

void *f1(void *_){
        //printf("Starting thread %d\n", n);
        entering1 = 1;
        number1 = 1 + largest(number0, number1, number2, number3);
        entering1 = 0;

        while(entering0){}
        while ( number0 != 0  &&
                ( number0 < number1 || ( number0 == number1 && 0 < 1) ) ){}

        while(entering1){}
        while ( number1 != 0  &&
                ( number1 < number1 || ( number1 == number1 && 1 < 1) ) ){}

        while(entering2){}
        while ( number2 != 0  &&
                ( number2 < number1 || ( number2 == number1 && 2 < 1) ) ){}

        while(entering3){}
        while ( number3 != 0  &&
                ( number3 < number1 || ( number3 == number1 && 3 < 1) ) ){}

        /* critical section */
        int y = x;
        y++;
        x = y;

        number1 = 0;

        //printf("Ending thread %d\n", n);
}

void *f2(void *_){
        //printf("Starting thread %d\n", n);
        entering2 = 1;
        number2 = 1 + largest(number0, number1, number2, number3);
        entering2 = 0;

        while(entering0){}
        while ( number0 != 0  &&
                ( number0 < number2 || ( number0 == number2 && 0 < 2) ) ){}

        while(entering1){}
        while ( number1 != 0  &&
                ( number1 < number2 || ( number1 == number2 && 1 < 2) ) ){}

        while(entering2){}
        while ( number2 != 0  &&
                ( number2 < number2 || ( number2 == number2 && 2 < 2) ) ){}

        while(entering3){}
        while ( number3 != 0  &&
                ( number3 < number2 || ( number3 == number2 && 3 < 2) ) ){}

        /* critical section */
        int y = x;
        y++;
        x = y;

        number2 = 0;

        //printf("Ending thread %d\n", n);
}

void *f3(void *_){
        //printf("Starting thread %d\n", n);
        entering3 = 1;
        number3 = 1 + largest(number0, number1, number2, number3);
        entering3 = 0;

        while(entering0){}
        while ( number0 != 0  &&
                ( number0 < number3 || ( number0 == number3 && 0 < 3) ) ){}

        while(entering1){}
        while ( number1 != 0  &&
                ( number1 < number3 || ( number1 == number3 && 1 < 3) ) ){}

        while(entering2){}
        while ( number2 != 0  &&
                ( number2 < number3 || ( number2 == number3 && 2 < 3) ) ){}

        while(entering3){}
        while ( number3 != 0  &&
                ( number3 < number3 || ( number3 == number3 && 3 < 3) ) ){}

        /* critical section */
        int y = x;
        y++;
        x = y;

        number3 = 0;

        //printf("Ending thread %d\n", n);
}

int main (){
        // thread t0(f, 0);
        // thread t1(f, 1);
        // thread t2(f, 2);
        // thread t3(f, 3);
        // t0.join();
        // t1.join();
        // t2.join();
        // t3.join();
    pthread_t t0, t1, t2, t3;
    // pthread_t t0, t1;
    pthread_create(&t0, 0, f0, 0);
    pthread_create(&t1, 0, f1, 0);
    pthread_create(&t2, 0, f2, 0);
    pthread_create(&t3, 0, f3, 0);
    pthread_join(t0, 0);
    pthread_join(t1, 0);
    pthread_join(t2, 0);
    pthread_join(t3, 0);

    //printf("x %d\n", x);
    return 0;
}

