//@ ltl invariant positive: [](AP(x == 0) ==> <>AP(x == 1));

#include <stdio.h>
#include <pthread.h>
#include <assert.h>
// #define N 2
typedef unsigned long int pthread_t;

int x = 0;
int number0, number1;
int entering0, entering1;

int largest(int number0, int number1)
{
    //int i;
    // Initialize maximum element 
    int max = number0;
    //for (i = 1; i < n; i++)
    if (number1 > max)
        max = number1;
    return max;
}



void *f0(void *_){
        //printf("Starting thread %d\n", n);
        entering0 = 1;
        number0 = 1 + largest(number0, number1);
        entering0 = 0;

        while(entering0){}
        while ( number0 != 0  &&
                ( number0 < number0 || ( number0 == number0 && 0 < 0) ) ){}

        while(entering1){}
        while ( number1 != 0  &&
                ( number1 < number0 || ( number1 == number0 && 1 < 0) ) ){}

        

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
        number1 = 1 + largest(number0, number1);
        entering1 = 0;

        while(entering0){}
        while ( number0 != 0  &&
                ( number0 < number1 || ( number0 == number1 && 0 < 1) ) ){}

        while(entering1){}
        while ( number1 != 0  &&
                ( number1 < number1 || ( number1 == number1 && 1 < 1) ) ){}
        /* critical section */
        int y = x;
        y++;
        x = y;

        number1 = 0;

        //printf("Ending thread %d\n", n);
}

// void *f2(void *_){
//         //printf("Starting thread %d\n", n);
//         entering[2] = 1;
//         number[2] = 1 + largest(number, N);
//         entering[2] = 0;

//         for(int j = 0; j < N; j++)
//         {
//                 while(entering[j]){}
//                 while ( number[j] != 0  &&
//                 ( number[j] < number[2] || ( number[j] == number[2] && j < 2 ) ) ){}
//         }

//         /* critical section */
//         int y = x;
//         y++;
//         x = y;

//         number[2] = 0;

//         //printf("Ending thread %d\n", n);
// }

int main (){
        // thread t0(f, 0);
        // thread t1(f, 1);
        // thread t2(f, 2);
        // thread t3(f, 3);
        // t0.join();
        // t1.join();
        // t2.join();
        // t3.join();
    // pthread_t t0, t1, t2;
    pthread_t t0, t1;
    pthread_create(&t0, 0, f0, 0);
    pthread_create(&t1, 0, f1, 0);
    // pthread_create(&t2, 0, f2, 0);
    pthread_join(t0, 0);
    pthread_join(t1, 0);
    // pthread_join(t2, 0);

    //printf("x %d\n", x);
    return 0;
}

