use mydatabase
db.dropDatabase()
db.patient.insertMany([
  {
    prename: 'John',
    surname: 'Wick' ,
    birthdate: '01.12.1989' ,
    insurance: {
              healthinsuranceNumber: 12323 ,
              healthinsurance: 'AOK' ,
              costs: {
                  amount: 235 ,
                  currency: 'euro'
                  },
        }
  },
  {
    prename: 'Anna',
    surname: 'Lisa' ,
    birthdate: '23.06.1950' ,
    insurance: {
                  healthinsuranceNumber: 11225 ,
                  healthinsurance: 'DEVK' ,
                  costs: {
                    amount: 330.50 ,
                    currency: 'euro'
                  },
        }  
  },
  
  {
    prename: 'Emma',
    surname: 'Kamm' ,
    birthdate: '16.04.1996' ,
    insurance: {
                  healthinsuranceNumber: 12345 ,
                  healthinsurance: 'BKK' ,
                  costs: {
                    amount: 198.95 ,
                    currency: 'euro'
                  },
        }  
  },
  
    {
    prename: 'Karl',
    surname: 'Müller' ,
    birthdate: '02.04.1967' ,
    insurance: {
                  healthinsuranceNumber: 12346 ,
                  healthinsurance: 'BKK' ,
                  costs: {
                    amount: 220.23 ,
                    currency: 'euro'
                  },
        }  
  },
      {
    prename: 'Alexander',
    surname: 'Fischer' ,
    birthdate: '24.05.1996' ,
    insurance: {
                  healthinsuranceNumber: 13578 ,
                  healthinsurance: 'AOK' ,
                  costs: {
                    amount: 220.23 ,
                    currency: 'euro'
                  },
        }  
  }
  
])
  